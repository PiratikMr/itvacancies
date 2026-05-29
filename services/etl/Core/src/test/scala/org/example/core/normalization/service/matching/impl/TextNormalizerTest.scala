package org.example.core.normalization.service.matching.impl

import org.example.SparkEnv
import org.example.core.normalization.engine.similarity.impl.TextNormalizer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class TextNormalizerTest extends AnyFlatSpec with Matchers with SparkEnv with TableDrivenPropertyChecks {

  import spark.implicits._

  "normalize" should "clean, stem and tokenize input" in {
    val scenarios = Table(
      ("input", "expected"),
      ("Senior Java Developer", Seq("_senior_", "_java_", "_develop_")),
      ("C++ Developer", Seq("_c++_", "_develop_")),
      ("  Java   17  ", Seq("_java_", "_17_")),
      ("Ёлка", Seq("_елк_")),
      (null, Seq.empty[String]),
      ("", Seq.empty[String])
    )

    forAll(scenarios) { (input, expected) =>
      val df = Seq(input).toDF("text")
      val res = df.select(TextNormalizer.normalize($"text")).head()

      val tokens = if (res.isNullAt(0)) Seq.empty[String] else res.getSeq[String](0)

      tokens should contain theSameElementsAs expected
    }
  }
}
