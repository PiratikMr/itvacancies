package org.example.finder.implement

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

class FinderTransformer(dbAdapter: DataBaseAdapter,
                        fuzzyConf: FuzzyMatcherConf) extends Transformer {

  override def toRows(spark: SparkSession, rawDS: Dataset[String]): DataFrame =
    spark.read.schema(FinderTransformer.schema).json(rawDS)

  override def transform(spark: SparkSession, rawDF: DataFrame): Dataset[Vacancy] = {
    import spark.implicits._

    rawDF
      .select(
        col("id").cast(StringType).as(VacancyColumns.EXTERNAL_ID),
        lit("Finder").as(VacancyColumns.PLATFORM),
        col("company.title").as(VacancyColumns.EMPLOYER),
        col("currency_symbol").as(VacancyColumns.CURRENCY),
        col("experience").as(VacancyColumns.EXPERIENCE),

        col("place.lat").cast(DoubleType).as(VacancyColumns.LATITUDE),
        col("place.lon").cast(DoubleType).as(VacancyColumns.LONGITUDE),

        col("salary_from").cast(DoubleType).as(VacancyColumns.SALARY_FROM),
        col("salary_to").cast(DoubleType).as(VacancyColumns.SALARY_TO),

        to_timestamp(col("publication_at")).as(VacancyColumns.PUBLISHED_AT),
        col("title").as(VacancyColumns.TITLE),
        col("description").as(VacancyColumns.DESCRIPTION),
        concat(lit("https://finder.work/vacancies/"), col("id")).as(VacancyColumns.URL),

        array(col("employment_type")).as(VacancyColumns.EMPLOYMENTS),
        typedLit(Seq.empty[String]).as(VacancyColumns.SCHEDULES),
        typedLit(Seq.empty[String]).as(VacancyColumns.GRADES),
        typedLit(Seq.empty[String]).as(VacancyColumns.SKILLS),
        typedLit(Seq.empty[org.example.core.etl.model.Language]).as(VacancyColumns.LANGUAGES),

        functions.transform(
          col("locations"),
          loc => struct(
            loc.getField("name").as(VacancyColumns.LOCATION),
            loc.getField("country").getField("name").as(VacancyColumns.COUNTRY)
          )
        ).as(VacancyColumns.LOCATIONS),

        col("professions.title").as(VacancyColumns.FIELDS)
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
        ExtractTags(SCHEDULES, VacancyColumns.DESCRIPTION),
        ExtractTags(SKILLS, VacancyColumns.DESCRIPTION),
        ExtractTags(GRADES, VacancyColumns.DESCRIPTION)
      ), transformedData)
  }
}

object FinderTransformer {

  val schema: StructType = StructType(Seq(
    StructField("id", LongType),
    StructField("title", StringType),
    StructField("employment_type", StringType),
    StructField("salary_from", LongType),
    StructField("salary_to", LongType),
    StructField("currency_symbol", StringType),
    StructField("publication_at", StringType),
    StructField("experience", StringType),
    StructField("distant_work", BooleanType),
    StructField("company", StructType(Seq(
      StructField("title", StringType)
    ))),
    StructField("locations", ArrayType(StructType(Seq(
      StructField("name", StringType),
      StructField("country", StructType(Seq(
        StructField("name", StringType)
      ))),
    )))),
    StructField("place", StructType(Seq(
      StructField("lat", StringType),
      StructField("lon", StringType),
    ))),
    StructField("professions", ArrayType(StructType(Seq(
      StructField("title", StringType)
    )))),
    StructField("description", StringType)
  ))

}
