package org.example.core.normalization.engine

import org.apache.spark.sql.{Column, Dataset}
import org.example.SparkEnv
import org.example.core.normalization.engine.model.{FuzzyCandidate, FuzzyDictionary, FuzzyMatch}
import org.example.core.normalization.engine.similarity.SimilarityStrategy
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class BroadcastTagExtractorTest extends AnyFlatSpec with Matchers with SparkEnv with TableDrivenPropertyChecks {

  import spark.implicits._

  private val dummySimilarityStrategy = new SimilarityStrategy {
    override def normalize(col: Column): Column = col

    override def buildFeatures(c: Column): Column = c

    override def calculateScore(c1: Column, c2: Column): Column = org.apache.spark.sql.functions.lit(1.0)
  }

  private val extractor = new BroadcastTagExtractor(spark, dummySimilarityStrategy)

  private def buildDict(data: Seq[(Long, String)]): Dataset[FuzzyDictionary] = {
    data.map { case (id, normVal) => FuzzyDictionary(id, normVal, -1L) }.toDS()
  }

  private def buildCand(data: Seq[(String, String)]): Dataset[FuzzyCandidate] = {
    data.map { case (entityId, rawVal) => FuzzyCandidate(entityId, rawVal, -1L) }.toDS()
  }

  "extractExactTags" should "return empty dataset when dictionary is empty" in {
    val candDs = buildCand(Seq("c1" -> "_java_ _developer_"))
    val dictDs = spark.emptyDataset[FuzzyDictionary]

    val result = extractor.extractExactTags(candDs, dictDs).collect()

    result shouldBe empty
  }

  it should "return empty dataset when candidates are empty" in {
    val candDs = spark.emptyDataset[FuzzyCandidate]
    val dictDs = buildDict(Seq(1L -> "_java_"))

    val result = extractor.extractExactTags(candDs, dictDs).collect()

    result shouldBe empty
  }

  it should "correctly extract tags covering various text scenarios" in {
    val scenarios = Table(
      ("candidates", "dictionary", "expected"),

      (
        Seq("c1" -> "_senior_ _java_ _developer_"),
        Seq(1L -> "_java_"),
        Seq(FuzzyMatch("c1", 1L))
      ),

      (
        Seq("c1" -> "_developer_ _java_ _senior_"),
        Seq(1L -> "_java_ _senior_"),
        Seq(FuzzyMatch("c1", 1L))
      ),

      (
        Seq("c1" -> "_developer_ _senior_ _java_"),
        Seq(1L -> "_java_ _senior_"),
        Seq(FuzzyMatch("c1", 1L))
      ),

      (
        Seq("c1" -> "_python_ _developer_ _java_ _c++_"),
        Seq(1L -> "_java_", 2L -> "_python_"),
        Seq(FuzzyMatch("c1", 1L), FuzzyMatch("c1", 2L))
      ),

      (
        Seq("c1" -> "_frontend_ _developer_"),
        Seq(1L -> "_java_"),
        Seq.empty[FuzzyMatch]
      ),

      (
        Seq("c1" -> "_java_", "c2" -> "_python_ _c++_"),
        Seq(1L -> "_java_", 2L -> "_python_"),
        Seq(FuzzyMatch("c1", 1L), FuzzyMatch("c2", 2L))
      ),

      (
        Seq("c1" -> null, "c2" -> "   ", "c3" -> ""),
        Seq(1L -> "_java_"),
        Seq.empty[FuzzyMatch]
      ),

      (
        Seq("c1" -> "_java_ _java_ _java_"),
        Seq(1L -> "_java_"),
        Seq(FuzzyMatch("c1", 1L))
      ),

      (
        Seq("c1" -> "_analyst_ _system_"),
        Seq(1L -> "_analyst_ _system_", 2L -> "_analyst_ _system_"),
        Seq(FuzzyMatch("c1", 1L), FuzzyMatch("c1", 2L))
      )
    )

    forAll(scenarios) { (candData, dictData, expected) =>
      val candDs = buildCand(candData)
      val dictDs = buildDict(dictData)

      val result = extractor.extractExactTags(candDs, dictDs).collect()

      result should contain theSameElementsAs expected
    }
  }
}