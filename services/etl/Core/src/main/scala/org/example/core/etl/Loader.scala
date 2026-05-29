package org.example.core.etl

import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.etl.model.NormalizedVacancy

trait Loader {

  def load(spark: SparkSession, ds: Dataset[NormalizedVacancy]): Unit

}