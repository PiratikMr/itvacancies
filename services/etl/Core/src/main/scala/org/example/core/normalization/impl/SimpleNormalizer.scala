package org.example.core.normalization.impl

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.etl.model.{Vacancy, VacancyColumns}
import org.example.core.normalization.api.{BaseNormalizer, TagExtractor}
import org.example.core.normalization.model.NormalizersEnum.GroupNonHierarchical
import org.example.core.normalization.model.{NormCandidate, NormMatch, NormalizationColumns}
import org.example.core.normalization.service.NormalizeService

class SimpleNormalizer(
                        spark: SparkSession,
                        service: NormalizeService,
                        nType: GroupNonHierarchical
                      ) extends BaseNormalizer with TagExtractor {

  import spark.implicits._

  private def getCandidatesForProcess(ds: Dataset[Vacancy]): Dataset[NormCandidate] = {
    val df = ds.toDF()
    val rawDf = if (nType.isArray) {
      df.select(
        col(VacancyColumns.EXTERNAL_ID).as(NormalizationColumns.ENTITY_ID),
        explode_outer(col(nType.sourceCol)).as(NormalizationColumns.RAW_VALUE)
      )
    } else {
      df.select(
        col(VacancyColumns.EXTERNAL_ID).as(NormalizationColumns.ENTITY_ID),
        col(nType.sourceCol).cast("string").as(NormalizationColumns.RAW_VALUE)
      )
    }

    rawDf
      .withColumn(NormalizationColumns.PARENT_ID, lit(null).cast("string"))
      .filter(col(NormalizationColumns.RAW_VALUE).isNotNull)
      .as[NormCandidate]
  }

  private def getCandidatesForExtract(ds: Dataset[Vacancy], sourceCol: String): Dataset[NormCandidate] = {
    ds.toDF()
      .select(
        col(VacancyColumns.EXTERNAL_ID).as(NormalizationColumns.ENTITY_ID),
        col(sourceCol).cast("string").as(NormalizationColumns.RAW_VALUE),
        lit(null).cast("string").as(NormalizationColumns.PARENT_ID)
      )
      .filter(col(NormalizationColumns.RAW_VALUE).isNotNull)
      .as[NormCandidate]
  }

  private def buildResult(mappedDs: Dataset[NormMatch]): DataFrame = {
    if (nType.isArray) {
      mappedDs.toDF()
        .groupBy(NormalizationColumns.ENTITY_ID)
        .agg(collect_list(NormalizationColumns.MAPPED_ID).as(nType.mappedIdCol))
    } else {
      mappedDs.toDF()
        .select(
          col(NormalizationColumns.ENTITY_ID),
          col(NormalizationColumns.MAPPED_ID).as(nType.mappedIdCol)
        )
    }
  }

  override def process(ds: Dataset[Vacancy], withCreate: Boolean): DataFrame = {
    val candidates = getCandidatesForProcess(ds)
    val mapped = service.mapSimple(candidates, withCreate)
    buildResult(mapped)
  }

  override def extractTags(ds: Dataset[Vacancy], sourceCol: String): DataFrame = {
    val candidates = getCandidatesForExtract(ds, sourceCol)
    val mappedTags = service.extractTags(candidates)
    buildResult(mappedTags)
  }
}