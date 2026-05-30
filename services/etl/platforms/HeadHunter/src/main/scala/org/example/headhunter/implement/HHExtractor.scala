package org.example.headhunter.implement

import org.apache.spark.sql.functions.{col, explode}
import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.adapter.web.WebAdapter
import org.example.core.adapter.web.impl.sttp.model.HttpError
import org.example.core.etl.Extractor

class HHExtractor(
                   profRolesIds: Dataset[Long],
                   apiBaseUrl: String,
                   dateFrom: String,
                   pageLimit: Int,
                   vacsPerPage: Int,
                   netPartition: Int,
                   rawPartition: Int
                 ) extends Extractor {

  override def extract(spark: SparkSession, webService: WebAdapter): Dataset[String] = {

    import spark.implicits._

    val _apiBaseUrl = apiBaseUrl
    val _dateFrom = dateFrom
    val _pageLimit = pageLimit
    val _vacsPerPage = vacsPerPage


    val clusterURLs: Dataset[String] = profRolesIds.mapPartitions(part => part.flatMap(role_id => {
      val body: String = webService.readBody(
        HHExtractor.clusterURL(_apiBaseUrl, _dateFrom, role_id, 0, 0)
      ).getOrElse("""{"found":0}""")

      val found: Int = """"found"\s*:\s*(\d+)""".r.findFirstMatchIn(body).get.group(1).toInt

      if (found == 0) Nil
      else (0 to math.min(_pageLimit, found / _vacsPerPage))
        .map(page => HHExtractor.clusterURL(_apiBaseUrl, _dateFrom, role_id, page, _vacsPerPage))
    }))

    val clusterData: Dataset[String] = clusterURLs.mapPartitions(part => part.flatMap(url =>
      webService.readBodyOrNone(url)
    )).persist()

    val schema: StructType = StructType(Seq(StructField("items", ArrayType(StructType(Seq(StructField("id", StringType)))))))

    val vacURLs: Dataset[String] = spark.read.schema(schema)
      .json(clusterData).select(explode(col("items.id")).as("id")).dropDuplicates("id")
      .map(row => HHExtractor.vacancyURL(_apiBaseUrl, row.getString(0)))

    vacURLs.repartition(netPartition).mapPartitions(part => part.flatMap(url =>
      webService.readBodyOrNone(url)
    )).repartition(rawPartition)
  }

  override def filterActiveVacancies(spark: SparkSession, activeIds: Dataset[String], webService: WebAdapter): Dataset[String] = {
    import spark.implicits._

    val _apiBaseUrl = apiBaseUrl

    activeIds.repartition(netPartition).mapPartitions(part => part.flatMap(id => {

      webService.execute(HHExtractor.vacancyURL(_apiBaseUrl, id)) match {
        case Right(response) if response.body.contains(""""archived":true""") => Some(id)
        case Left(HttpError(404, _)) => Some(id)
        case _ => None
      }
    }))
  }

}

private object HHExtractor {
  private def clusterURL(apiBaseUrl: String, dateFrom: String, roleId: Long, page: Long, perPage: Long): String =
    s"$apiBaseUrl/vacancies?page=$page&per_page=$perPage&professional_role=$roleId&date_from=$dateFrom"

  private def vacancyURL(apiBaseUrl: String, id: String): String = s"$apiBaseUrl/vacancies/$id"
}