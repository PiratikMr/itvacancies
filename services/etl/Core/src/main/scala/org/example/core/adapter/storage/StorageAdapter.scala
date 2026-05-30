package org.example.core.adapter.storage

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

trait StorageAdapter {

  def read(spark: SparkSession, folderName: String, withDate: Boolean): DataFrame

  def readText(spark: SparkSession, folderName: String): Dataset[String]

  def write(df: DataFrame, folderName: String, withDate: Boolean): Unit

  def writeText(ds: Dataset[String], folderName: String): Unit

}