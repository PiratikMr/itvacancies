package org.example.habrcareer.implement

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, functions}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.model.structures.FuzzyMatcherConf
import org.example.core.etl.Transformer
import org.example.core.etl.model.{Language, NormalizedVacancy, Vacancy, VacancyColumns}
import org.example.core.normalization.model.NormalizersEnum._
import org.example.core.normalization.service.NormalizationOrchestrator

class HabrTransformer(
                       dbAdapter: DataBaseAdapter,
                       fuzzyConf: FuzzyMatcherConf
                     ) extends Transformer {

  override def toRows(spark: SparkSession, rawDS: Dataset[String]): DataFrame =
    spark.read.schema(StructType(Seq(StructField("list", ArrayType(HabrTransformer.scheme))))).json(rawDS)

  override def transform(spark: SparkSession, rawDF: DataFrame): Dataset[Vacancy] = {
    import spark.implicits._

    rawDF.select(explode(col("list")).as("lst")).select("lst.*")
      .select(
        col("id").cast(StringType).as(VacancyColumns.EXTERNAL_ID),
        lit("HabrCareer").as(VacancyColumns.PLATFORM),
        col("company.title").as(VacancyColumns.EMPLOYER),
        when(upper(col("salary.currency")) === "RUR", lit("RUB"))
          .when(upper(col("salary.currency")) === "BYR", lit("BYN"))
          .otherwise(upper(col("salary.currency"))).as(VacancyColumns.CURRENCY),
        lit(null).cast(StringType).as(VacancyColumns.EXPERIENCE),

        col("salary.from").cast(DoubleType).as(VacancyColumns.SALARY_FROM),
        col("salary.to").cast(DoubleType).as(VacancyColumns.SALARY_TO),

        lit(null).cast(DoubleType).as(VacancyColumns.LATITUDE),
        lit(null).cast(DoubleType).as(VacancyColumns.LONGITUDE),

        to_timestamp(col("publishedDate.date"), "yyyy-MM-dd'T'HH:mm:ssXXX").as(VacancyColumns.PUBLISHED_AT),
        col("title").as(VacancyColumns.TITLE),
        lit(null).cast(StringType).as(VacancyColumns.DESCRIPTION),
        concat(lit("https://career.habr.com/vacancies/"), col("id")).as(VacancyColumns.URL),

        array(
          when(col("remoteWork") === true, lit("Удаленная работа")).otherwise(lit(null))
        ).as(VacancyColumns.SCHEDULES),
        array(
          when(col("employment").isNotNull, col("employment")).otherwise(lit(null))
        ).as(VacancyColumns.EMPLOYMENTS),

        functions.transform(
          col("locations"),
          loc => struct(
            loc.getField("title").as(VacancyColumns.LOCATION),
            lit(null).cast(StringType).as(VacancyColumns.COUNTRY)
          )
        ).as(VacancyColumns.LOCATIONS),

        functions.transform(col("divisions"), f => f.getField("title")).as(VacancyColumns.FIELDS),
        functions.transform(col("skills"), s => s.getField("title")).as(VacancyColumns.SKILLS),
        array(col("salaryQualification.title")).as(VacancyColumns.GRADES),
        typedLit(Seq.empty[Language]).as(VacancyColumns.LANGUAGES)

      ).as[Vacancy]
  }

  override def normalize(spark: SparkSession, transformedData: Dataset[Vacancy]): Dataset[NormalizedVacancy] = {
    new NormalizationOrchestrator(spark, dbAdapter, fuzzyConf)
      .normalize(Seq(
        CURRENCY,
        EMPLOYER,
        EMPLOYMENTS,
        EXPERIENCE,
        FIELDS,
        LOCATIONS,
        PLATFORM,
        SCHEDULES,
        SKILLS,
        GRADES
      ), transformedData)
  }
}

object HabrTransformer {
  private val scheme: StructType = StructType(Seq(
    StructField("id", LongType),
    StructField("title", StringType),
    StructField("remoteWork", BooleanType),
    StructField("salaryQualification", StructType(Seq(
      StructField("title", StringType)
    ))),
    StructField("publishedDate", StructType(Seq(
      StructField("date", StringType)
    ))),
    StructField("company", StructType(Seq(
      StructField("title", StringType)
    ))),
    StructField("employment", StringType),
    StructField("salary", StructType(Seq(
      StructField("from", LongType),
      StructField("to", LongType),
      StructField("currency", StringType)
    ))),
    StructField("divisions", ArrayType(StructType(Seq(
      StructField("title", StringType)
    )))),
    StructField("skills", ArrayType(StructType(Seq(
      StructField("title", StringType)
    )))),
    StructField("locations", ArrayType(StructType(Seq(
      StructField("title", StringType)
    ))))
  ))
}