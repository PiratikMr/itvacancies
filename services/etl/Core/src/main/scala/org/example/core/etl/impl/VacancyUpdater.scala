package org.example.core.etl.impl

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.functions.{current_timestamp, lit}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.database.{DimPlatformDef, FactVacancyDef}

class VacancyUpdater(spark: SparkSession, dbAdapter: DataBaseAdapter) extends LazyLogging {

  import spark.implicits._

  private val factDef = FactVacancyDef
  private val dimPlatformDef = DimPlatformDef


  def getActiveVacancies(limit: Int, platformName: String): Dataset[String] = {

    getPlatformId(platformName) match {
      case None =>
        spark.emptyDataset[String]

      case Some(id) =>
        val activeQuery =
          s"""
             |SELECT ${factDef.externalId} as id
             |FROM ${factDef.meta.tableName}
             |WHERE ${factDef.platformId} = $id AND ${factDef.closedAt} IS NULL
             |ORDER BY ${factDef.publishedAt} ASC
             |LIMIT $limit
             |""".stripMargin

        dbAdapter.loadQuery(spark, activeQuery).as[String]
    }
  }

  def updateVacancies(unActiveIds: Dataset[String], platformName: String): Unit = {
    if (unActiveIds.isEmpty) {
      logger.info(s"Среди проверенных вакансий платформы $platformName закрытых не найдено")
      return
    }

    getPlatformId(platformName) match {
      case None => ()

      case Some(id) =>
        val toUpdateDf = unActiveIds.toDF(factDef.externalId)
          .withColumn(factDef.platformId, lit(id))
          .withColumn(factDef.closedAt, current_timestamp())

        dbAdapter.update(
          df = toUpdateDf,
          targetTable = factDef.meta.tableName,
          joinColumns = Seq(factDef.externalId, factDef.platformId),
          updateColumns = Seq(factDef.closedAt)
        )
    }
  }


  private def getPlatformId(platformName: String): Option[Long] = {

    val getQuery =
      s"""
         |SELECT ${dimPlatformDef.entityId}
         |FROM ${dimPlatformDef.meta.tableName}
         |WHERE ${dimPlatformDef.entityName} = '$platformName'
         |""".stripMargin

    val platformDf = dbAdapter.loadQuery(spark, getQuery)

    if (platformDf.isEmpty) {
      logger.error(s"Платформа '$platformName' не найдена в таблице ${dimPlatformDef.meta.tableName}")
      return None
    }

    Some(platformDf.head().getLong(0))
  }

}
