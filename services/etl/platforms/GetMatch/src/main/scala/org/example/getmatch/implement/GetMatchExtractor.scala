package org.example.getmatch.implement

import org.apache.spark.sql.functions.{col, explode}
import org.apache.spark.sql.types.{ArrayType, LongType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.web.WebAdapter
import org.example.core.etl.Extractor
import org.example.getmatch.config.GetMatchFileConfig

class GetMatchExtractor(
                         conf: GetMatchFileConfig,
                         apiBaseUrl: String,
                         netPartition: Int,
                         rawPartition: Int
                       ) extends Extractor {

  override def extract(spark: SparkSession, webAdapter: WebAdapter): Dataset[String] = {

    import spark.implicits._

    val _apiBaseUrl = apiBaseUrl

    val totalUrls: Int = math.min({
      val body: String = webAdapter.readBody(
        GetMatchExtractor.clusterURL(conf, _apiBaseUrl, perPage = 1),
      ).getOrElse("""{"total":0}""")

      """"total"\s*:\s*(\d+)""".r.findFirstMatchIn(body).get.group(1).toInt
    }, conf.vacsLimit) / conf.vacsPerPage


    val clusterURLs: Dataset[String] = (0 to totalUrls)
      .map(page => GetMatchExtractor.clusterURL(conf, _apiBaseUrl, page = page))
      .toDS().repartition(netPartition)

    val clustersDS: Dataset[String] = clusterURLs
      .mapPartitions(part => part.flatMap(url => webAdapter.readBodyOrNone(url)))

    val vacancyIdsDF: DataFrame = spark.read.schema(StructType(Seq(
        StructField("offers", ArrayType(StructType(Seq(StructField("id", LongType)))))
      )))
      .json(clustersDS)
      .select(explode(col("offers")).as("offer"))
      .select(col("offer.id").as("id"))
      .repartition(netPartition)


    vacancyIdsDF.mapPartitions(part => part.flatMap(row => {
      webAdapter.readBodyOrNone(GetMatchExtractor.vacancyURL(_apiBaseUrl, row.getLong(0).toString))
    })).repartition(rawPartition)
  }

  override def filterActiveVacancies(spark: SparkSession, activeIds: Dataset[String], webAdapter: WebAdapter): Dataset[String] = {

    import spark.implicits._

    val _apiBaseUrl = apiBaseUrl

    activeIds.repartition(netPartition).mapPartitions(part => part.flatMap(id => {
      val body: String = webAdapter.readBody(
        GetMatchExtractor.vacancyURL(_apiBaseUrl, id)
      ).getOrElse(""""is_active":true""")
      """\s*"is_active":\s*false\s*""".r.findFirstMatchIn(body) match {
        case Some(_) => Some(id)
        case _ => None
      }
    }))
  }
}

object GetMatchExtractor {

  private def clusterURL(conf: GetMatchFileConfig, apiBaseUrl: String, perPage: Int = -1, page: Int = 0): String = {
    val pp: Int = if (perPage < 0) conf.vacsPerPage else perPage
    val offset: Int = page * pp
    val l: Int = if (offset + pp > conf.vacsLimit) conf.vacsLimit - offset else pp

    s"$apiBaseUrl/offers?pa=${conf.inDays}&limit=$l&offset=$offset"
  }

  private def vacancyURL(apiBaseUrl: String, id: String): String = s"$apiBaseUrl/offers/$id"

}
