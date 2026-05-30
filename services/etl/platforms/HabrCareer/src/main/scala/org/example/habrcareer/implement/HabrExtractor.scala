package org.example.habrcareer.implement

import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.adapter.web.WebAdapter
import org.example.core.etl.Extractor

class HabrExtractor(
                     apiBaseUrl: String,
                     vacsPageLimit: Int,
                     vacsPerPage: Int,
                     netRepartition: Int,
                     rawPartition: Int
                   ) extends Extractor {

  override def extract(spark: SparkSession, webService: WebAdapter): Dataset[String] = {
    import spark.implicits._

    val totalVacs: Int = math.min(vacsPageLimit * vacsPerPage, {
      val body: String = webService.readBodyOrNone(
        HabrExtractor.vacanciesURL(apiBaseUrl, 1, 0)
      ).getOrElse(s"""{"totalPages":0}""")
      """"totalPages"\s*:\s*(\d+)""".r.findFirstMatchIn(body).get.group(1).toInt
    })

    val urlsDS: Dataset[String] = (0 to (totalVacs / vacsPerPage))
      .map(page => HabrExtractor.vacanciesURL(apiBaseUrl, vacsPerPage, page))
      .toDS()
      .repartition(netRepartition)

    urlsDS.mapPartitions(part => part.flatMap(url => {
      webService.readBodyOrNone(url)
    })).repartition(rawPartition)
  }

  override def filterActiveVacancies(spark: SparkSession, activeIds: Dataset[String], webService: WebAdapter): Dataset[String] =
    activeIds
}

private object HabrExtractor {
  private def vacanciesURL(apiBaseUrl: String, perPage: Int, page: Int): String =
    s"$apiBaseUrl/frontend/vacancies?sort=date&type=all&per_page=$perPage&page=$page"
}