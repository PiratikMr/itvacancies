package org.example.core.normalization.engine.similarity.impl

import org.apache.spark.sql.functions._
import org.example.SparkEnv
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.TableDrivenPropertyChecks

class TextNormalizerTest extends AnyFlatSpec with SparkEnv with TableDrivenPropertyChecks {

  import spark.implicits._

  "TextNormalizer" should "normalize text by stemming and filtering correctly" in {
    val data = Table(
      ("input", "expectedTokens"),
      ("java dev", Seq("_java_", "_dev_")),
      ("  extra   spaces  ", Seq("_extra_", "_space_")),
      ("https://example.com/link some #текст", Seq("_some_", "_#текст_")),
      ("", Seq.empty[String]),
      (null, Seq.empty[String])
    )

    forAll(data) { (input, expectedTokens) =>
      val df = Seq(input).toDF("text")
      val actual = df.withColumn("normalized", TextNormalizer.normalize(col("text")))
        .select("normalized").as[Seq[String]].head()
      if (input == null || input.trim.isEmpty) {
        actual shouldBe Seq.empty
      } else {
        actual.length shouldBe expectedTokens.length
        actual.foreach { token =>
          assert(token.startsWith("_"))
          assert(token.endsWith("_"))
        }
      }
    }
  }
}
