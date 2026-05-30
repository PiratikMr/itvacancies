package org.example.core.adapter.database.impl.postgres

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.model.structures.DBConf

class PostgresAdapter(conf: DBConf) extends DataBaseAdapter with LazyLogging {

  override def loadTable(spark: SparkSession, targetTable: String): DataFrame = {
    logger.debug(s"PostgresAdapter: вызов loadTable для таблицы $targetTable")
    PostgresUtils.loadTable(spark, conf, targetTable)
  }

  override def loadQuery(spark: SparkSession, query: String): DataFrame = {
    logger.debug(s"PostgresAdapter: вызов loadQuery")
    PostgresUtils.loadQuery(spark, conf, query)
  }

  override def save(df: DataFrame, targetTable: String, conflicts: Seq[String], updates: Option[Seq[String]] = None): Unit = {
    logger.debug(s"PostgresAdapter: вызов save для таблицы $targetTable")
    PostgresUtils.save(conf, df, targetTable, conflicts, updates)
  }

  override def update(df: DataFrame, targetTable: String, joinColumns: Seq[String], updateColumns: Seq[String]): Unit = {
    logger.debug(s"PostgresAdapter: вызов update для таблицы $targetTable")
    PostgresUtils.update(conf, df, targetTable, joinColumns, updateColumns)
  }

  override def saveWithReturn(spark: SparkSession, df: DataFrame, targetTable: String, returns: Seq[String], conflicts: Seq[String], updates: Option[Seq[String]] = None): DataFrame = {
    logger.debug(s"PostgresAdapter: вызов saveWithReturn для таблицы $targetTable")
    PostgresUtils.saveWithReturn(spark, conf, df, targetTable, returns, conflicts, updates)
  }
}