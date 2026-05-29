package org.example.core.normalization.service

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.SparkEnv
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.database.{DimDef, MappingDimDef}
import org.example.core.config.model.structures.FuzzyMatchSettings
import org.example.core.normalization.engine.FuzzyMatcher
import org.example.core.normalization.engine.model._
import org.example.core.normalization.model.{NormCandidate, NormalizationColumns}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar

class NormalizeServiceTest extends AnyFlatSpec with SparkEnv with MockitoSugar {

  import spark.implicits._

  private val dbAdapterMock = mock[DataBaseAdapter]
  private val fuzzyMatcherMock = mock[FuzzyMatcher]

  private val settings = FuzzyMatchSettings(0.7, 1.0, 0.5, 0.5, 3)
  private val dimDef = new DimDef("test", Some("parent_id"))
  private val mappingDef = new MappingDimDef("test")

  private val normalizeService = new NormalizeService(
    spark,
    dbAdapterMock,
    settings,
    dimDef,
    mappingDef,
    Some(fuzzyMatcherMock)
  )

  "NormalizeService" should "extract tags utilizing DataBaseAdapter and FuzzyMatcher" in {
    val fakeMappingData = Seq(
      (100L, "fake_norm", true, 1L)
    ).toDF(
      "id", "norm_value", "is_canonical", "parent_id"
    )

    when(dbAdapterMock.loadQuery(any[SparkSession], any[String])).thenReturn(fakeMappingData)

    val fakeMatchDf = Seq(
      FuzzyMatch("candidate1", 100L)
    ).toDS()

    when(fuzzyMatcherMock.extractTags(any[Dataset[FuzzyCandidate]], any[Dataset[FuzzyDictionary]]))
      .thenReturn(fakeMatchDf)

    val candidates = Seq(NormCandidate("candidate1", "some value", Some("1"))).toDS()
    val res = normalizeService.extractTags(candidates).collect()

    res.length shouldBe 1
    res.head.entityId shouldBe "candidate1"
    res.head.mappedId shouldBe 100L

    verify(dbAdapterMock, times(1)).loadQuery(any[SparkSession], any[String])
    verify(fuzzyMatcherMock, times(1)).extractTags(any[Dataset[FuzzyCandidate]], any[Dataset[FuzzyDictionary]])
  }

  "NormalizeService" should "mapSimple creating new dim entries" in {
    val emptyMappingData = spark.emptyDataset[(Long, String, Boolean, Long)].toDF(
      "id", "norm_value", "is_canonical", "parent_id"
    )
    when(dbAdapterMock.loadQuery(any[SparkSession], any[String])).thenReturn(emptyMappingData)

    val mockRes = FuzzyMatcherResult(
      matched = spark.emptyDataset[FuzzyMatch],
      toCreate = Seq(FuzzyToCreate("cand1", "val1", 1L)).toDS(),
      mappingData = Seq(FuzzyMappingMeta("nval1", isCanonical = true, "val1", 1L)).toDS(),
      clearCache = () => {}
    )

    when(fuzzyMatcherMock.execute(any[Dataset[FuzzyCandidate]], any[Dataset[FuzzyDictionary]]))
      .thenReturn(mockRes)

    val returnedDimDf = Seq(
      (200L, "val1", 1L)
    ).toDF("id", NormalizationColumns.RAW_VALUE, "parent_id")

    when(dbAdapterMock.saveWithReturn(
      any[SparkSession], any[DataFrame], any[String], any[Seq[String]], any[Seq[String]], any[Option[Seq[String]]]
    )).thenReturn(returnedDimDf)

    val candidates = Seq(NormCandidate("cand1", "val1", Some("1"))).toDS()
    val res = normalizeService.mapSimple(candidates, withCreate = true).collect()

    res.length shouldBe 1
    res.head.entityId shouldBe "cand1"
    res.head.mappedId shouldBe 200L

    verify(dbAdapterMock, times(1)).saveWithReturn(
      any[SparkSession], any[DataFrame], any[String], any[Seq[String]], any[Seq[String]], any[Option[Seq[String]]]
    )
    verify(dbAdapterMock, times(1)).save(
      any[DataFrame], any[String], any[Seq[String]], any[Option[Seq[String]]]
    )
  }

}
