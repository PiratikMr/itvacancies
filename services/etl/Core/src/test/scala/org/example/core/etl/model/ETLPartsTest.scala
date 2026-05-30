package org.example.core.etl.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class ETLPartsTest extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  "ETLParts.parse" should "parse known ETL parts case-insensitively" in {
    val scenarios = Table(
      ("input", "expected"),
      ("extract",        ETLParts.Extract),
      ("EXTRACT",        ETLParts.Extract),
      ("  extract  ",    ETLParts.Extract),
      ("transform-load", ETLParts.TransformLoad),
      ("Transform-Load", ETLParts.TransformLoad),
      ("update",         ETLParts.Update),
      ("UPDATE",         ETLParts.Update)
    )

    forAll(scenarios) { (input, expected) =>
      ETLParts.parse(input).get shouldBe expected
    }
  }

  it should "fail for unknown values" in {
    val unknowns = Table("input", "load", "tl", "ext", "unknown", "")

    forAll(unknowns) { input =>
      ETLParts.parse(input).isFailure shouldBe true
    }
  }

  it should "include the invalid input in the error message" in {
    val result = ETLParts.parse("not-a-part")
    result.isFailure shouldBe true
    result.failed.get shouldBe a[IllegalArgumentException]
    result.failed.get.getMessage should include("not-a-part")
  }
}
