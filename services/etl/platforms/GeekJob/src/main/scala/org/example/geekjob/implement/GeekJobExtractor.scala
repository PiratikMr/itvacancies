package org.example.geekjob.implement

import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.adapter.web.WebAdapter
import org.example.core.etl.Extractor

class GeekJobExtractor(apiBaseUrl: String,
                       pageLimit: Int,
                       netPartition: Int,
                       rawPartitions: Int) extends Extractor {

  override def extract(spark: SparkSession, webAdapter: WebAdapter): Dataset[String] = {

    import spark.implicits._

    val _apiBaseUrl = apiBaseUrl

    val firstPageContent: String = webAdapter.readBodyOrThrow(GeekJobExtractor.pageURL(_apiBaseUrl, 1))

    val pageURLs: Dataset[String] = {
      val totalPages: Int = """<small>страниц (\d+)</small>""".r.findFirstMatchIn(firstPageContent).get.group(1).toInt
      val pagesToProcess: Int = math.min(totalPages, pageLimit)

      (2 to pagesToProcess).map(page => GeekJobExtractor.pageURL(_apiBaseUrl, page))
    }.toDS

    val vacancyIDs: Dataset[String] = pageURLs.repartition(netPartition)
      .mapPartitions(part => part.flatMap(url => {
        webAdapter.readBodyOrNone(url) match {
          case Some(body) => """/vacancy/([a-z0-9]{24})""".r.findAllMatchIn(body).map(_.group(1))
          case None => None
        }
      })).dropDuplicates()


    vacancyIDs.mapPartitions(part => part.flatMap(id => {
      webAdapter.readBodyOrNone(GeekJobExtractor.vacURL(_apiBaseUrl, id)) match {
        case Some(body) =>
          val start: Int = body.indexOf("""<article class="row vacancy">""")
          if (start == -1) None
          else {
            val bodyEnd: String = "</article>"
            val endIdx: Int = body.indexOf(bodyEnd, start)
            if (endIdx == -1) None
            else Some(id + body.substring(start, endIdx + bodyEnd.length).replace("\n", ""))
          }
        case None => None
      }
    })).repartition(rawPartitions)

  }

  override def filterActiveVacancies(spark: SparkSession, activeIds: Dataset[String], webAdapter: WebAdapter): Dataset[String] = {
    import spark.implicits._

    val _apiBaseUrl = apiBaseUrl

    activeIds.repartition(netPartition).mapPartitions(part => part.flatMap(id =>
      webAdapter.readBodyOrNone(GeekJobExtractor.vacURL(_apiBaseUrl, id)) match {
        case Some(body) if body.contains("Эта вакансия была перемещена в архив.") => Some(id)
        case _ => None
      }
    ))
  }
}

private object GeekJobExtractor {

  private def pageURL(apiBaseUrl: String, page: Int): String = s"$apiBaseUrl/vacancies/$page"

  private def vacURL(apiBaseUrl: String, code: String): String = s"$apiBaseUrl/vacancy/$code"
}
