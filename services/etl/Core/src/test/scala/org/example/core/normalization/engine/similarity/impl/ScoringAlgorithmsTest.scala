package org.example.core.normalization.engine.similarity.impl

import org.apache.spark.sql.functions._
import org.example.SparkEnv
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.TableDrivenPropertyChecks

class ScoringAlgorithmsTest extends AnyFlatSpec with SparkEnv with TableDrivenPropertyChecks {

  import spark.implicits._

  "ScoringAlgorithms" should "build n-grams correctly" in {
    val data = Seq(
      ("hello", Seq("hel", "ell", "llo")),
      ("he", Seq.empty[String]),
      (null, Seq.empty[String])
    ).toDF("input", "expected")

    val result = data
      .withColumn("actual", ScoringAlgorithms.buildNGrams(col("input"), 3))
      .select("expected", "actual")
      .as[(Seq[String], Seq[String])]
      .collect()

    result.foreach { case (expected, actual) =>
      actual shouldBe expected
    }
  }

  "ScoringAlgorithms" should "extract numbers correctly" in {
    val data = Table(
      ("input", "expected"),
      ("model 123", Seq("123")),
      ("abc 12 def 34", Seq("12", "34")),
      ("no numbers here", Seq.empty[String]),
      (null, Seq.empty[String])
    )

    forAll(data) { (input, expected) =>
      val df = Seq(input).toDF("input")
      val actual = df.withColumn("numbers", ScoringAlgorithms.extractNumbers(col("input")))
        .select("numbers").as[Seq[String]].head()

      if (actual == null) expected shouldBe Seq.empty
      else actual shouldBe expected
    }
  }

  "ScoringAlgorithms" should "calculate levenshtein score properly" in {
    val data = Table(
      ("s1", "s2", "expectedScore"),
      ("kitten", "sitting", 1.0 - (3.0 / 7.0)),
      ("same", "same", 1.0),
      ("", "", 1.0)
    )

    forAll(data) { (s1, s2, expectedScore) =>
      val df = Seq((s1, s2)).toDF("s1", "s2")
      val actual = df.withColumn("score", ScoringAlgorithms.levenshteinScore(col("s1"), col("s2")))
        .select("score").as[Double].head()

      assert(math.abs(actual - expectedScore) < 0.001)
    }
  }

  "ScoringAlgorithms" should "calculate ngram score from arrays correctly" in {
    val data = Table(
      ("arr1", "arr2", "expectedScore"),
      (Seq("a", "b"), Seq("b", "c"), 2.0 * 1 / (2 + 2)),
      (Seq("a"), Seq("a"), 1.0),
      (Seq.empty[String], Seq.empty[String], 0.0)
    )

    forAll(data) { (arr1, arr2, expectedScore) =>
      val df = Seq((arr1, arr2)).toDF("a1", "a2")
      val actual = df.withColumn("score", ScoringAlgorithms.nGramScoreFromArrays(col("a1"), col("a2")))
        .select("score").as[Double].head()

      assert(math.abs(actual - expectedScore) < 0.001)
    }
  }

}
