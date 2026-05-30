package org.example.core.etl.utils

import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions._
import org.example.core.etl.model.{Vacancy, VacancyColumns}

object DataTransformer {

  def normalizeSkills(ds: Dataset[Vacancy]): Dataset[Vacancy] = {
    import ds.sparkSession.implicits._

    val regex = """[,/\\;•]"""

    ds.withColumn(
      VacancyColumns.SKILLS,
      array_distinct(
        filter(
          transform(
            flatten(
              transform(
                coalesce(col(VacancyColumns.SKILLS), array().cast("array<string>")),
                skill => split(skill, regex)
              )
            ),
            skill => trim(skill)
          ),
          skill => length(skill) > 0
        )
      )
    ).as[Vacancy]
  }

}
