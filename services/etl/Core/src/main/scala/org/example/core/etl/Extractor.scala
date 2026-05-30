package org.example.core.etl

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.web.WebAdapter

trait Extractor {

  def extract(spark: SparkSession, webService: WebAdapter): Dataset[String]

  def filterActiveVacancies(spark: SparkSession, activeIds: Dataset[String], webService: WebAdapter): Dataset[String]

}