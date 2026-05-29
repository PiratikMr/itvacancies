package org.example.core.normalization.engine

import org.apache.spark.sql.Column
import org.apache.spark.sql.functions._
import org.example.SparkEnv
import org.example.core.normalization.engine.model.{FuzzyCandidate, FuzzyDictionary, FuzzyMatch}
import org.example.core.normalization.engine.similarity.SimilarityStrategy
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.TableDrivenPropertyChecks

class FuzzyMatcherTest extends AnyFlatSpec with SparkEnv with TableDrivenPropertyChecks {

  import spark.implicits._

  object MockStrategy extends SimilarityStrategy {
    override def normalize(col: Column): Column = {
      trim(lower(col))
    }

    override def buildFeatures(c: Column): Column = {
      struct(
        c.as("text"),
        split(c, " ").as("ngrams"),
        array().cast("array<double>").as("numbers")
      )
    }

    override def calculateScore(c1: Column, c2: Column): Column = {
      when(c1.getField("text") === c2.getField("text"), lit(1.0))
        .otherwise(lit(0.8))
    }
  }

  private val matcher = new FuzzyMatcher(spark, MockStrategy, 0.7)

  private def runFuzzyExecuteTest(
                                   testName: String,
                                   candidates: Seq[FuzzyCandidate],
                                   dictionary: Seq[FuzzyDictionary],
                                   expectedMatches: Seq[FuzzyMatch],
                                   expectedCreatesCount: Int
                                 ): Unit = {
    val result = matcher.execute(candidates.toDS(), dictionary.toDS())

    val actualMatches = result.matched.collect().sortBy(m => (m.entityId, m.dictId)).toList
    val expectedSorted = expectedMatches.sortBy(m => (m.entityId, m.dictId)).toList

    actualMatches shouldBe expectedSorted
    result.toCreate.count() shouldBe expectedCreatesCount
  }

  "FuzzyMatcher" should "execute matching using parameterized inputs and mocked strategy" in {
    val scenarios = Table(
      ("testName", "candidates", "dictionary", "expectedMatches", "expectedCreatesCount"),
      (
        "Exact Match",
        Seq(FuzzyCandidate("c1", "java backend", 1L)),
        Seq(FuzzyDictionary(100L, "java backend", 1L)),
        Seq(FuzzyMatch("c1", 100L)),
        0
      ),
      (
        "Fuzzy Match - Strategy Returns 0.8 Score",
        Seq(FuzzyCandidate("c1", "java backend", 1L)),
        Seq(FuzzyDictionary(100L, "java backend!!", 1L)),
        Seq(FuzzyMatch("c1", 100L)),
        0
      ),
      (
        "Parent ID constraint prevents match",
        Seq(FuzzyCandidate("c1", "java backend", 2L)),
        Seq(FuzzyDictionary(100L, "java backend", 1L)),
        Seq.empty[FuzzyMatch],
        1
      ),
      (
        "Self Matching - Synonyms creation",
        Seq(
          FuzzyCandidate("c1", "python developer", 1L),
          FuzzyCandidate("c2", "python developer", 1L),
          FuzzyCandidate("c3", "java backend", 1L)
        ),
        Seq.empty[FuzzyDictionary],
        Seq.empty[FuzzyMatch],
        3
      )
    )

    forAll(scenarios) { (testName, candidates, dictionary, expectedMatches, expectedCreatesCount) =>
      withClue(s"Scenario: $testName") {
        runFuzzyExecuteTest(testName, candidates, dictionary, expectedMatches, expectedCreatesCount)
      }
    }
  }

}
