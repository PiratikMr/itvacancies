package org.example.core.config.model.structures

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class FSConfTest extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  private val conf = FSConf(
    url = "hdfs://namenode:9000",
    rootPath = "data/etl",
    platform = "headhunter",
    dateFolder = "2024-01-15"
  )

  "FSConf.getPath" should "include date folder when withDate is true" in {
    conf.getPath("Vacancies") shouldBe "hdfs://namenode:9000/data/etl/headhunter/Vacancies/2024-01-15"
  }

  it should "omit date folder when withDate is false" in {
    conf.getPath("Vacancies", withDate = false) shouldBe "hdfs://namenode:9000/data/etl/headhunter/Vacancies"
  }

  it should "default to withDate=true" in {
    conf.getPath("Raw") shouldBe conf.getPath("Raw", withDate = true)
  }

  it should "build paths correctly for different folder names" in {
    val scenarios = Table(
      ("folderName", "withDate", "expected"),
      ("Vacancies", true,  "hdfs://namenode:9000/data/etl/headhunter/Vacancies/2024-01-15"),
      ("Raw",       true,  "hdfs://namenode:9000/data/etl/headhunter/Raw/2024-01-15"),
      ("Vacancies", false, "hdfs://namenode:9000/data/etl/headhunter/Vacancies"),
      ("Archive",   false, "hdfs://namenode:9000/data/etl/headhunter/Archive")
    )

    forAll(scenarios) { (folder, withDate, expected) =>
      conf.getPath(folder, withDate) shouldBe expected
    }
  }
}
