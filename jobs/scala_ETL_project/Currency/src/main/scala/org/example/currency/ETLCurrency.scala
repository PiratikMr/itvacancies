package org.example.currency

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DoubleType, MapType, StringType, TimestampType}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.adapter.storage.StorageAdapter
import org.example.core.adapter.web.WebAdapter
import org.example.core.config.database.{DimCurrencyDef, DimCurrencyRateHistoryDef, MappingCurrencyDef}
import org.example.core.config.model.structures.FuzzyMatchSettings
import org.example.core.etl.model.ETLParts
import org.example.core.etl.model.ETLParts.{Extract, TransformLoad}
import org.example.core.normalization.engine.similarity.impl.DefaultSimilarityStrategy

import java.sql.Timestamp
import scala.util.{Failure, Success}

class ETLCurrency(
                   spark: SparkSession,
                   dbAdapter: DataBaseAdapter,
                   storageAdapter: StorageAdapter,
                   webAdapter: WebAdapter,
                   apiBaseUrl: String,
                   apiKey: String,
                   fuzzySettings: FuzzyMatchSettings
                 ) {

  import spark.implicits._

  private val dimDef = DimCurrencyDef
  private val historyDef = DimCurrencyRateHistoryDef
  private val mappingDef = MappingCurrencyDef

  private val FolderName = "Rates"

  def run(etlPart: String): Unit =
    ETLParts.parse(etlPart) match {
      case Success(Extract) => extractStep()

      case Success(TransformLoad) => transformLoadStep()

      case Success(part) =>
        throw new UnsupportedOperationException(s"ETL часть '${part.cliName}' не поддерживается для Currency")

      case Failure(e) => throw e
    }

  private def extractStep(): Unit = {
    val body = webAdapter.readBodyOrThrow(s"$apiBaseUrl/$apiKey/latest/RUB")

    val compactDS = spark.read.option("multiLine", "true").json(Seq(body).toDS()).toJSON

    storageAdapter.writeText(compactDS, FolderName)
  }

  private def transformLoadStep(): Unit = {
    val rawDS = storageAdapter.readText(spark, FolderName)
    val df = transform(rawDS)
    load(df)
  }

  private def transform(rawDS: Dataset[String]): DataFrame = {
    val rawDF = spark.read.json(rawDS)

    val apiTimestamp: Timestamp = rawDF
      .select(col("time_last_update_unix").cast(TimestampType))
      .as[Timestamp]
      .first()

    rawDF
      .select(
        explode(
          from_json(
            to_json(col("conversion_rates")),
            MapType(StringType, DoubleType)
          )
        ).as(Seq(dimDef.entityName, historyDef.rate))
      )
      .withColumn(historyDef.updateDate, lit(apiTimestamp))
  }

  private def load(df: DataFrame): Unit = {
    val similarityStrategy = new DefaultSimilarityStrategy(fuzzySettings)

    val savedDimDf = dbAdapter.saveWithReturn(
      spark = spark,
      df = df.select(dimDef.entityName),
      targetTable = dimDef.meta.tableName,
      returns = Seq(dimDef.entityId, dimDef.entityName),
      conflicts = dimDef.meta.conflictKeys,
      updates = None
    ).cache()

    val historyDf = savedDimDf
      .join(df.select(dimDef.entityName, historyDef.rate, historyDef.updateDate), Seq(dimDef.entityName))
      .select(historyDef.currencyId, historyDef.updateDate, historyDef.rate)

    dbAdapter.save(
      df = historyDf,
      targetTable = historyDef.meta.tableName,
      conflicts = historyDef.meta.conflictKeys,
      updates = Some(Seq(historyDef.rate))
    )

    val mappingDf = savedDimDf
      .withColumn(mappingDef.mappedValue, similarityStrategy.normalize(col(dimDef.entityName)))
      .withColumn(mappingDef.isCanonical, lit(true))
      .select(mappingDef.entityId, mappingDef.mappedValue, mappingDef.isCanonical)

    dbAdapter.save(
      df = mappingDf,
      targetTable = mappingDef.meta.tableName,
      conflicts = mappingDef.meta.conflictKeys
    )

    savedDimDf.unpersist(blocking = false)
  }
}
