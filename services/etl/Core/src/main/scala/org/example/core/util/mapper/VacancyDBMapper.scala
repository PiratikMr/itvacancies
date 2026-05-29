package org.example.core.util.mapper

import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.config.database.FactVacancyDef
import org.example.core.etl.model.NormalizedVacancy
import org.example.core.etl.model.VacancyColumns._

object VacancyDBMapper {

  def toFactVacancyTable(spark: SparkSession, ds: Dataset[NormalizedVacancy]): Dataset[FactVacancyDef.Record] = {
    import spark.implicits._

    ds.select(
      lit(null).cast(LongType).as(FactVacancyDef.vacancyId),

      $"$EXTERNAL_ID".as(FactVacancyDef.externalId),
      $"$PLATFORM_ID".as(FactVacancyDef.platformId),
      $"$EMPLOYER_ID".as(FactVacancyDef.employerId),
      $"$CURRENCY_ID".as(FactVacancyDef.currencyId),
      $"$EXPERIENCE_ID".as(FactVacancyDef.experienceId),
      $"$LATITUDE".as(FactVacancyDef.latitude),
      $"$LONGITUDE".as(FactVacancyDef.longitude),
      $"$SALARY_FROM".as(FactVacancyDef.salaryFrom),
      $"$SALARY_TO".as(FactVacancyDef.salaryTo),
      $"$PUBLISHED_AT".as(FactVacancyDef.publishedAt),
      $"$TITLE".as(FactVacancyDef.title),
      $"$URL".as(FactVacancyDef.url)
    ).as[FactVacancyDef.Record]
  }

}
