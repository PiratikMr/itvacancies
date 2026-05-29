package org.example.headhunter.implement

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, functions}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.model.structures.FuzzyMatcherConf
import org.example.core.etl.Transformer
import org.example.core.etl.model.VacancyColumns._
import org.example.core.etl.model.{NormalizedVacancy, Vacancy, VacancyColumns}
import org.example.core.normalization.api.NormalizationTask.ExtractTags
import org.example.core.normalization.model.NormalizersEnum
import org.example.core.normalization.service.NormalizationOrchestrator
import org.example.headhunter.implement.HHTransformer.normalizedSalary

class HHTransformer(
                     areas: DataFrame,
                     dbAdapter: DataBaseAdapter,
                     fuzzyConf: FuzzyMatcherConf
                   ) extends Transformer {

  override def toRows(spark: SparkSession, rawDS: Dataset[String]): DataFrame =
    spark.read.schema(HHTransformer.scheme).json(rawDS)

  override def transform(spark: SparkSession, df: DataFrame): Dataset[Vacancy] = {

    val dfWithAreas = df.join(
      areas,
      df("area.id") === areas("area_id"),
      "left"
    )

    import spark.implicits._

    dfWithAreas
      .withColumn("parsed_published_at", to_timestamp(col("published_at"), "yyyy-MM-dd'T'HH:mm:ss+0300"))

      .select(
        col("id").as(EXTERNAL_ID),
        col("name").as(VacancyColumns.TITLE),
        col("description").as(VacancyColumns.DESCRIPTION),
        col("alternate_url").as(VacancyColumns.URL),
        lit("HeadHunter").as(VacancyColumns.PLATFORM),

        col("address.lat").as(VacancyColumns.LATITUDE),
        col("address.lng").as(VacancyColumns.LONGITUDE),

        normalizedSalary(col("salary_range.from"), col("salary_range.mode.id")).as(SALARY_FROM),
        normalizedSalary(col("salary_range.to"), col("salary_range.mode.id")).as(SALARY_TO),

        col("employer.name").as(VacancyColumns.EMPLOYER),
        col("salary_range.currency").as(VacancyColumns.CURRENCY),
        col("experience.name").as(VacancyColumns.EXPERIENCE),

        array(col("employment_form.name")).as(VacancyColumns.EMPLOYMENTS),
        col("work_format.name").as(VacancyColumns.SCHEDULES),
        col("professional_roles.name").as(VacancyColumns.FIELDS),
        typedLit(Seq.empty[String]).as(VacancyColumns.GRADES),
        col("key_skills.name").as(VacancyColumns.SKILLS),

        functions.transform(
          col("languages"),
          lang => struct(
            lang.getField("level").getField("name").as(VacancyColumns.LEVEL),
            lang.getField("name").as(VacancyColumns.LANGUAGE)
          )
        ).as(VacancyColumns.LANGUAGES),
        array(
          struct(
            col("country").as(VacancyColumns.COUNTRY),
            col("region").as(VacancyColumns.LOCATION)
          )
        ).as(VacancyColumns.LOCATIONS),

        col("parsed_published_at").as(VacancyColumns.PUBLISHED_AT),
      ).as[Vacancy]
  }

  override def normalize(spark: SparkSession, transformedData: Dataset[Vacancy]): Dataset[NormalizedVacancy] = {

    new NormalizationOrchestrator(spark, dbAdapter, fuzzyConf)
      .normalize(Seq(
        NormalizersEnum.CURRENCY,
        NormalizersEnum.EMPLOYER,
        NormalizersEnum.EMPLOYMENTS,
        NormalizersEnum.EXPERIENCE,
        NormalizersEnum.FIELDS,
        NormalizersEnum.LOCATIONS,
        NormalizersEnum.PLATFORM,
        NormalizersEnum.SCHEDULES,
        NormalizersEnum.SKILLS,
        NormalizersEnum.LANGUAGES,
        ExtractTags(NormalizersEnum.GRADES, VacancyColumns.DESCRIPTION)
      ), transformedData)
  }
}

object HHTransformer {

  private val AVG_WORK_HOURS_PER_MONTH: Int = 166

  import org.apache.spark.sql.Column
  import org.apache.spark.sql.functions.when

  private def normalizedSalary(salaryCol: Column, modeIdCol: Column): Column =
    when(salaryCol.isNotNull && modeIdCol === "HOUR", salaryCol * AVG_WORK_HOURS_PER_MONTH)
      .otherwise(salaryCol)

  private val scheme = StructType(Seq(

    StructField("address", StructType(Seq(
      StructField("lat", DoubleType),
      StructField("lng", DoubleType),
      StructField("metro_stations", ArrayType(StructType(Seq(
        StructField("station_name", StringType)
      ))))
    ))),

    StructField("alternate_url", StringType),

    //    StructField("archived", BooleanType),

    StructField("area", StructType(Seq(
      StructField("id", StringType)
    ))),

    //    StructField("driver_license_types", ArrayType(StructType(Seq(
    //      StructField("id", StringType)
    //    )))),

    StructField("employer", StructType(Seq(
      StructField("id", StringType),
      StructField("name", StringType),
      StructField("trusted", BooleanType)
    ))),

    StructField("employment_form", StructType(Seq(
      StructField("name", StringType)
    ))),

    StructField("experience", StructType(Seq(
      StructField("name", StringType)
    ))),

    StructField("id", StringType),

    StructField("key_skills", ArrayType(StructType(Seq(
      StructField("name", StringType)
    )))),

    StructField("languages", ArrayType(StructType(Seq(
      StructField("level", StructType(Seq(
        StructField("name", StringType)
      ))),
      StructField("name", StringType)
    )))),

    StructField("name", StringType),

    //    StructField("night_shifts", BooleanType),

    StructField("professional_roles", ArrayType(StructType(Seq(
      StructField("name", StringType)
    )))),

    StructField("description", StringType),

    StructField("published_at", StringType),

    StructField("salary_range", StructType(Seq(
      StructField("currency", StringType),
      StructField("frequency", StructType(Seq(
        StructField("name", StringType),
      ))),
      StructField("from", DoubleType),
      StructField("gross", BooleanType),
      StructField("mode", StructType(Seq(
        StructField("id", StringType),
      ))),
      StructField("to", DoubleType)
    ))),

    StructField("work_format", ArrayType(StructType(Seq(
      StructField("name", StringType)
    ))))
  ))
}