package org.example.core.normalization.service

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.database.{DimDef, MappingDimDef}
import org.example.core.config.model.structures.FuzzyMatchSettings
import org.example.core.normalization.engine.FuzzyMatcher
import org.example.core.normalization.engine.model.{FuzzyCandidate, FuzzyColumns, FuzzyDictionary}
import org.example.core.normalization.model.{NormCandidate, NormMatch, NormalizationColumns}
import org.example.core.normalization.service.NormalizeService._

class NormalizeService(
                        spark: SparkSession,
                        dbAdapter: DataBaseAdapter,
                        settings: FuzzyMatchSettings,
                        dimDef: DimDef,
                        mappingDef: MappingDimDef,
                        fuzzyMatcherOpt: Option[FuzzyMatcher] = None
                      ) {

  import spark.implicits._

  private val fuzzyMatcher = fuzzyMatcherOpt.getOrElse(FuzzyMatcher(spark, settings))


  // id, value, parent_id
  private def saveDimTable(df: DataFrame, valueCol: String): DataFrame = {

    val colsToWrite = Seq(col(valueCol).as(dimDef.entityName)) ++
      dimDef.parentId.map(name => col(parentId).as(name))

    val returnCols = Seq(dimDef.entityId, dimDef.entityName) ++ dimDef.parentId.toSeq

    val saved = dbAdapter.saveWithReturn(
      spark = spark,
      df = df.select(colsToWrite: _*),
      targetTable = dimDef.meta.tableName,
      returns = returnCols,
      conflicts = dimDef.meta.conflictKeys
    )

    val res = saved
      .withColumnRenamed(dimDef.entityId, mappedId)
      .withColumnRenamed(dimDef.entityName, valueCol)

    dimDef.parentId match {
      case Some(name) => res.withColumnRenamed(name, parentId)
      case None => res.withColumn(parentId, lit(DEFAULT_PARENT_ID))
    }
  }


  // [id, norm_value, is_canonical, parent_id]
  private def loadFullMappingTable(): DataFrame = {

    val parentSelect = dimDef.parentId.map(n => s"d.$n").getOrElse(DEFAULT_PARENT_ID.toString)

    val query =
      s"""
         |SELECT  d.${dimDef.entityId} as $mappedId,
         |        md.${mappingDef.mappedValue} as $normValue,
         |        md.${mappingDef.isCanonical} as $isCanonical,
         |        $parentSelect as $parentId
         |FROM ${mappingDef.meta.tableName} as md
         |JOIN ${dimDef.meta.tableName} as d on d.${dimDef.entityId} = md.${mappingDef.entityId}
         |""".stripMargin

    dbAdapter.loadQuery(spark, query)
  }


  def extractTags(candidates: Dataset[NormCandidate]): Dataset[NormMatch] = {

    val fullMappingTable = loadFullMappingTable().cache() // [id, norm_value, is_canonical, parent_id]

    if (fullMappingTable.isEmpty) {
      return spark.emptyDataset[NormMatch]
    }

    val dictionaryDs = fullMappingTable
      .select(
        col(mappedId).as(FuzzyColumns.DICT_ID),
        col(normValue).as(FuzzyColumns.NORM_VALUE),
        col(parentId).as(FuzzyColumns.PARENT_ID)
      ).as[FuzzyDictionary]

    val rawCandidates = candidates.toDF()
      .withColumn(FuzzyColumns.PARENT_ID, coalesce(col(NormalizationColumns.PARENT_ID), lit(DEFAULT_PARENT_ID)))
      .select(
        col(NormalizationColumns.ENTITY_ID).as(FuzzyColumns.ENTITY_ID),
        col(NormalizationColumns.RAW_VALUE).as(FuzzyColumns.RAW_VALUE),
        col(FuzzyColumns.PARENT_ID)
      )
      .as[FuzzyCandidate]

    val exactMatchesDs = fuzzyMatcher.extractTags(rawCandidates, dictionaryDs)

    exactMatchesDs.toDF()
      .select(
        col(FuzzyColumns.ENTITY_ID).as(NormalizationColumns.ENTITY_ID),
        col(FuzzyColumns.DICT_ID).as(NormalizationColumns.MAPPED_ID)
      ).as[NormMatch]
  }


  def mapSimple(candidates: Dataset[NormCandidate], withCreate: Boolean): Dataset[NormMatch] = {

    val fullMappingTable = loadFullMappingTable().cache() // [id, norm_value, is_canonical, parent_id]

    val dictionaryDs = fullMappingTable
      .select(
        col(mappedId).as(FuzzyColumns.DICT_ID),
        col(normValue).as(FuzzyColumns.NORM_VALUE),
        col(parentId).as(FuzzyColumns.PARENT_ID)
      )
      .as[FuzzyDictionary]

    val rawCandidates = candidates.toDF()
      .withColumn(FuzzyColumns.PARENT_ID, coalesce(col(NormalizationColumns.PARENT_ID), lit(DEFAULT_PARENT_ID)))
      .select(
        col(NormalizationColumns.ENTITY_ID).as(FuzzyColumns.ENTITY_ID),
        col(NormalizationColumns.RAW_VALUE).as(FuzzyColumns.RAW_VALUE),
        col(FuzzyColumns.PARENT_ID)
      )
      .as[FuzzyCandidate]

    val fuzzyRes = fuzzyMatcher.execute(
      candidatesDs = rawCandidates,
      dictionaryDs = dictionaryDs
    )


    val matchedResult = fuzzyRes.matched.toDF()
      .select(
        col(FuzzyColumns.ENTITY_ID).as(NormalizationColumns.ENTITY_ID),
        col(FuzzyColumns.DICT_ID).as(NormalizationColumns.MAPPED_ID)
      )

    if (!withCreate || fuzzyRes.toCreate.isEmpty) {
      val checkPointedRes = matchedResult.as[NormMatch].localCheckpoint()
      fuzzyRes.clearCache()
      fullMappingTable.unpersist(blocking = false)
      return checkPointedRes
    }


    val newDimsToWrite = fuzzyRes.toCreate.toDF()
      .select(
        col(FuzzyColumns.RAW_VALUE).as(NormalizationColumns.RAW_VALUE),
        col(FuzzyColumns.PARENT_ID).as(parentId)
      )

    val reloadedDims = saveDimTable(newDimsToWrite, NormalizationColumns.RAW_VALUE).cache()

    val createdMapping = fuzzyRes.toCreate.toDF().alias("create")
      .join(reloadedDims.alias("dim"),
        col("create.rawValue") === col(s"dim.${NormalizationColumns.RAW_VALUE}") &&
          col("create.parentId") === col(s"dim.$parentId")
      )
      .select(
        col("create.entityId").as(NormalizationColumns.ENTITY_ID),
        col(s"dim.$mappedId").as(NormalizationColumns.MAPPED_ID)
      )

    if (!fuzzyRes.mappingData.isEmpty) {
      val mappingDataToWrite = fuzzyRes.mappingData.toDF().alias("map")
        .join(reloadedDims.alias("dim"),
          col("map.rawValue") === col(s"dim.${NormalizationColumns.RAW_VALUE}") &&
            col("map.parentId") === col(s"dim.$parentId")
        )
        .select(
          col("map.normValue").as(mappingDef.mappedValue),
          col("map.isCanonical").as(mappingDef.isCanonical),
          col(s"dim.$mappedId").as(mappingDef.entityId)
        )

      dbAdapter.save(mappingDataToWrite, mappingDef.meta.tableName, mappingDef.meta.conflictKeys)
    }

    val finalRes = matchedResult.union(createdMapping).distinct()

    val checkPointedRes = finalRes.as[NormMatch].localCheckpoint()
    fuzzyRes.clearCache()
    fullMappingTable.unpersist(blocking = false)
    reloadedDims.unpersist(blocking = false)

    checkPointedRes
  }

}

object NormalizeService {
  private val mappedId = "id"
  private val normValue = "norm_value"
  private val isCanonical = "is_origin"
  private val parentId = "parent_id"
  private val DEFAULT_PARENT_ID = -1L
}