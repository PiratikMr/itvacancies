package org.example.core.normalization.service.matching.impl

import org.example.SparkEnv
import org.example.core.normalization.engine.similarity.impl.ScoringAlgorithms
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScoringAlgorithmsTest extends AnyFlatSpec with Matchers with SparkEnv {

  import spark.implicits._

  "levenshteinScore" should "calculate normalized similarity" in {
    val df = Seq(("abc", "abd")).toDF("v1", "v2")
    val score = df.select(ScoringAlgorithms.levenshteinScore($"v1", $"v2")).head().getDouble(0)
    score shouldBe 0.666 +- 0.001
  }

  it should "handle exact match" in {
    val df = Seq(("test", "test")).toDF("v1", "v2")
    val score = df.select(ScoringAlgorithms.levenshteinScore($"v1", $"v2")).head().getDouble(0)
    score shouldBe 1.0
  }
}