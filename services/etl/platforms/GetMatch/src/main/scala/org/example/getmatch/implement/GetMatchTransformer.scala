package org.example.getmatch.implement

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, functions}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.model.structures.FuzzyMatcherConf
import org.example.core.etl.model.{NormalizedVacancy, Vacancy, VacancyColumns}
import org.example.core.etl.Transformer
import org.example.core.normalization.api.NormalizationTask.ExtractTags
import org.example.core.normalization.service.NormalizationOrchestrator
import org.example.core.normalization.model.NormalizersEnum._
import org.example.getmatch.implement.GetMatchTransformer._

class GetMatchTransformer(dbAdapter: DataBaseAdapter,
                          fuzzyConf: FuzzyMatcherConf) extends Transformer {


  override def toRows(spark: SparkSession, rawDS: Dataset[String]): DataFrame =
    spark.read.schema(schema).json(rawDS)

  override def transform(spark: SparkSession, rawDF: DataFrame): Dataset[Vacancy] = {
    import spark.implicits._

    val currencyMapColumn = functions.map(
      GetMatchTransformer.currencyMap.flatMap { case (k, v) => Seq(lit(k), lit(v)) }.toSeq: _*
    )

    rawDF.select(
      col("id").cast(StringType).as(VacancyColumns.EXTERNAL_ID),
      lit("GetMatch").as(VacancyColumns.PLATFORM),
      col("company.name").as(VacancyColumns.EMPLOYER),
      currencyMapColumn.getItem(col("salary_currency")).as(VacancyColumns.CURRENCY),
      col("required_years_of_experience").cast(StringType).as(VacancyColumns.EXPERIENCE),

      col("salary_display_from").cast(DoubleType).as(VacancyColumns.SALARY_FROM),
      col("salary_display_to").cast(DoubleType).as(VacancyColumns.SALARY_TO),
      lit(null).cast(DoubleType).as(VacancyColumns.LATITUDE),
      lit(null).cast(DoubleType).as(VacancyColumns.LONGITUDE),

      col("published_at").cast(TimestampType).as(VacancyColumns.PUBLISHED_AT),
      col("position").as(VacancyColumns.TITLE),
      col("offer_description").as(VacancyColumns.DESCRIPTION),
      concat(lit("https://getmatch.ru"), col("url")).as(VacancyColumns.URL),

      array(col("seniority")).as(VacancyColumns.GRADES),

      array_remove(
        array(
          when(col("remote_options").isNotNull, lit("Удаленная работа"))
            .otherwise(lit(null)),
          when(col("office_options").isNotNull, lit("Полный день"))
            .otherwise(lit(null))
        ),
        null
      ).as(VacancyColumns.SCHEDULES),
      typedLit(Seq.empty[String]).as(VacancyColumns.EMPLOYMENTS),
      typedLit(Seq.empty[String]).as(VacancyColumns.FIELDS),

      filter(functions.transform(
        array(col("english_level.name")),
        lang => struct(
          lit("Английский").as(VacancyColumns.LANGUAGE),
          lang.as(VacancyColumns.LEVEL)
        )
      ),
        x => x.getField(VacancyColumns.LEVEL).isNotNull
      ).as(VacancyColumns.LANGUAGES),

      functions.transform(
        col("display_locations"),
        loc => struct(
          loc.getField("city").as(VacancyColumns.LOCATION),
          loc.getField("country").as(VacancyColumns.COUNTRY)
        )
      ).as(VacancyColumns.LOCATIONS),

      flatten(
        functions.transform(
          col("stack"),
          stack_item => split(trim(stack_item), "\\s+/\\s+")
        )
      ).as(VacancyColumns.SKILLS)
    ).as[Vacancy]
  }

  override def normalize(spark: SparkSession, transformedData: Dataset[Vacancy]): Dataset[NormalizedVacancy] = {

    new NormalizationOrchestrator(spark, dbAdapter, fuzzyConf)
      .normalize(Seq(
        PLATFORM,
        EMPLOYER,
        CURRENCY,
        EXPERIENCE,
        GRADES,
        SCHEDULES,
        LANGUAGES,
        LOCATIONS,
        SKILLS
      ), transformedData)

  }
}

object GetMatchTransformer {

  private lazy val currencyMap = Map(
    "€" -> "EUR",
    "₽" -> "RUB",
    "$"-> "USD"
  )

  private val schema: StructType = StructType(Seq(
    StructField("published_at", StringType),
    StructField("position", StringType),
    StructField("offer_description", StringType),
    StructField("id", LongType),
    StructField("salary_display_from", LongType),
    StructField("salary_display_to", LongType),
    StructField("salary_currency", StringType),
    StructField("stack", ArrayType(StringType)),
    StructField("url", StringType),
    StructField("english_level", StructType(Seq(
      StructField("name", StringType)
    ))),
    StructField("remote_options", StringType),
    StructField("office_options", StringType),
    StructField("company", StructType(Seq(
      StructField("name", StringType)
    ))),
    StructField("seniority", StringType),
    StructField("required_years_of_experience", IntegerType),
    StructField("display_locations", ArrayType(StructType(Seq(
      StructField("city", StringType),
      StructField("country", StringType)
    ))))

  ))
}