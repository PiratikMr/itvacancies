package org.example.core.etl.utils

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, Dataset}
import org.example.core.etl.model.{Vacancy, VacancyColumns}

object VacancySanitizer {

  def applySanitize(ds: Dataset[Vacancy]): Dataset[Vacancy] = {
    import ds.sparkSession.implicits._

    ds
      .withColumn(VacancyColumns.SKILLS, transformSkills(col(VacancyColumns.SKILLS)))
      .withColumn(VacancyColumns.SALARY_FROM, transformSalary(col(VacancyColumns.SALARY_FROM)))
      .withColumn(VacancyColumns.SALARY_TO, transformSalary(col(VacancyColumns.SALARY_TO)))

      .as[Vacancy]
  }


  private def transformSkills(skills: Column): Column = {
    val regex = """[,/\\;•]"""

    array_distinct(
      filter(
        transform(
          flatten(
            transform(
              coalesce(skills, array().cast("array<string>")),
              skill => split(skill, regex)
            )
          ),
          skill => trim(skill)
        ),
        skill => length(skill) > 0
      )
    )
  }

  private def transformSalary(salary: Column): Column = {
    when(salary > 0, salary)
  }

}
