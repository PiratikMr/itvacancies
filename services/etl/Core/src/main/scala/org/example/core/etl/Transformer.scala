package org.example.core.etl

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.etl.model.{NormalizedVacancy, Vacancy}

trait Transformer {

  def toRows(spark: SparkSession, rawDS: Dataset[String]): DataFrame

  def transform(spark: SparkSession, rawDF: DataFrame): Dataset[Vacancy]

  def normalize(spark: SparkSession, transformedData: Dataset[Vacancy]): Dataset[NormalizedVacancy]

}
