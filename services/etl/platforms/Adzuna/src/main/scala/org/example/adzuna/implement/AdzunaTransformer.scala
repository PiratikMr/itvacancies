package org.example.adzuna.implement

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, functions}
import org.example.adzuna.implement.AdzunaTransformer.schema
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.model.structures.FuzzyMatcherConf
import org.example.core.etl.model.{Language, NormalizedVacancy, Vacancy, VacancyColumns}
import org.example.core.etl.Transformer
import org.example.core.normalization.model.NormalizersEnum._
import org.example.core.normalization.service.NormalizationOrchestrator

class AdzunaTransformer(
                         dbAdapter: DataBaseAdapter,
                         fuzzyConf: FuzzyMatcherConf,
                         currency: String,
                         urlDomain: String
                       ) extends Transformer {

  override def toRows(spark: SparkSession, rawDS: Dataset[String]): DataFrame =
    spark.read.schema(StructType(Seq(StructField("results", ArrayType(schema))))).json(rawDS)

  override def transform(spark: SparkSession, rawDF: DataFrame): Dataset[Vacancy] = {
    import spark.implicits._

    val _currency = currency
    val _urlDomain = urlDomain

    rawDF.select(explode(col("results")).as("rslt")).select("rslt.*")
      .select(
        col("id").cast(StringType).as(VacancyColumns.EXTERNAL_ID),
        lit("Adzuna").as(VacancyColumns.PLATFORM),
        col("company.display_name").as(VacancyColumns.EMPLOYER),
        lit(_currency).as(VacancyColumns.CURRENCY),
        lit(null).cast(StringType).as(VacancyColumns.EXPERIENCE),

        col("salary_min").cast(DoubleType).as(VacancyColumns.SALARY_FROM),
        col("salary_max").cast(DoubleType).as(VacancyColumns.SALARY_TO),

        col("latitude").cast(DoubleType).as(VacancyColumns.LATITUDE),
        col("longitude").cast(DoubleType).as(VacancyColumns.LONGITUDE),

        to_timestamp(col("created")).as(VacancyColumns.PUBLISHED_AT),
        col("title").as(VacancyColumns.TITLE),
        lit(null).cast(StringType).as(VacancyColumns.DESCRIPTION),
        concat(lit("https://www.adzuna."), lit(_urlDomain), lit("/details/"), col("id")).as(VacancyColumns.URL),

        array(col("contract_time")).as(VacancyColumns.EMPLOYMENTS),
        typedLit(Seq.empty[String]).as(VacancyColumns.SCHEDULES),
        typedLit(Seq.empty[String]).as(VacancyColumns.FIELDS),
        typedLit(Seq.empty[String]).as(VacancyColumns.SKILLS),
        typedLit(Seq.empty[String]).as(VacancyColumns.GRADES),
        typedLit(Seq.empty[Language]).as(VacancyColumns.LANGUAGES),

        array(
          struct(
            when(size(col("location.area")) > 1, element_at(col("location.area"), -1))
              .otherwise(lit(null).cast(StringType)).as(VacancyColumns.LOCATION),
            col("location.area").getItem(0).as(VacancyColumns.COUNTRY)
          )
        ).as(VacancyColumns.LOCATIONS)

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

object AdzunaTransformer {

  private val schema = StructType(Seq(
    StructField("id", StringType),
    StructField("title", StringType),
    StructField("created", StringType),
    StructField("latitude", DoubleType),
    StructField("longitude", DoubleType),
    StructField("location", StructType(Seq(
      StructField("area", ArrayType(StringType))
    ))),
    StructField("company", StructType(Seq(
      StructField("display_name", StringType)
    ))),
    StructField("salary_min", DoubleType),
    StructField("salary_max", DoubleType),
    StructField("contract_time", StringType)
  ))

}
