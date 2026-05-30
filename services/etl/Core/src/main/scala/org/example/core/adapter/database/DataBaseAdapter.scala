package org.example.core.adapter.database

import org.apache.spark.sql.{DataFrame, SparkSession}

trait DataBaseAdapter {

  def loadTable(
                 spark: SparkSession,
                 targetTable: String
               ): DataFrame

  def loadQuery(
                 spark: SparkSession,
                 query: String
               ): DataFrame

  def save(
            df: DataFrame,
            targetTable: String,
            conflicts: Seq[String],
            updates: Option[Seq[String]] = None
          ): Unit

  def update(
              df: DataFrame,
              targetTable: String,
              joinColumns: Seq[String],
              updateColumns: Seq[String]
            ): Unit

  def saveWithReturn(
                      spark: SparkSession,
                      df: DataFrame,
                      targetTable: String,
                      returns: Seq[String],
                      conflicts: Seq[String],
                      updates: Option[Seq[String]] = None
                    ): DataFrame

}