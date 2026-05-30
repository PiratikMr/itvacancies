package org.example.finder.implement

import org.apache.spark.sql.functions.{col, explode}
import org.apache.spark.sql.types.{ArrayType, LongType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.web.WebAdapter
import org.example.core.etl.Extractor
import org.example.finder.config.FinderFileConfig

class FinderExtractor(
                       finderConf: FinderFileConfig,
                       apiBaseUrl: String,
                       netRepartition: Int,
                       rawPartition: Int
                     ) extends Extractor {

  override def extract(spark: SparkSession, webAdapter: WebAdapter): Dataset[String] = {
    val _apiBaseUrl = apiBaseUrl

    val totalCount: Int = math.min({
      val body: String = webAdapter.readBody(FinderExtractor.clusterURL(finderConf, _apiBaseUrl, 0, 0))
        .getOrElse("""{"count": 0}""")

      """"count"\s*:\s*(\d+)""".r.findFirstMatchIn(body).get.group(1).toInt
    }, finderConf.vacsLimit)

    val totalUrls: Int = {
      val total = totalCount / finderConf.vacsPerPage

      if (totalCount % finderConf.vacsPerPage == 0)
        total - 1
      else
        total
    }

    import spark.implicits._

    val clusterUrlsDS: Dataset[String] = (0 to totalUrls)
      .map(value => FinderExtractor.clusterURL(finderConf, _apiBaseUrl, finderConf.vacsPerPage, finderConf.vacsPerPage * value))
      .toDS().repartition(netRepartition)

    val clustersDS: Dataset[String] = clusterUrlsDS.mapPartitions(part => {
      part.flatMap(url => webAdapter.readBodyOrNone(url))
    })

    val vacancyIdsDF: DataFrame = spark.read.schema(StructType(Seq(
        StructField("items", ArrayType(StructType(Seq(StructField("id", LongType)))))
      ))).json(clustersDS).select(explode(col("items")).as("item"))
      .select(col("item.id").as("id")).repartition(netRepartition)

    vacancyIdsDF.mapPartitions(part => part.map(row => FinderExtractor.vacancyURL(_apiBaseUrl, row.getLong(0).toString)))
      .mapPartitions(part => {
        part.flatMap(url => webAdapter.readBodyOrNone(url)).map(body => body.replace("\n", ""))
      }).repartition(rawPartition)
  }

  override def filterActiveVacancies(spark: SparkSession, activeIds: Dataset[String], webService: WebAdapter): Dataset[String] = {
    val _apiBaseUrl = apiBaseUrl

    import spark.implicits._

    activeIds.repartition(netRepartition).mapPartitions(part => part.flatMap(id => {

      val body = webService.readBody(FinderExtractor.vacancyURL(_apiBaseUrl, id))
        .getOrElse("""{"status": "pupu"}""")

      """\s*"status": "active"\s*""".r.findFirstMatchIn(body) match {
        case Some(_) => None
        case None => Some(id)
      }
    }))
  }
}

object FinderExtractor {

  private def vacancyURL(apiBaseUrl: String, id: String): String =
    s"$apiBaseUrl/vacancies/$id"

  private def clusterURL(
                          conf: FinderFileConfig,
                          apiBaseUrl: String,
                          l: Int,
                          offset: Int): String = {
    val limit: Int = if (offset + l > conf.vacsLimit) conf.vacsLimit - offset else l
    s"$apiBaseUrl/vacancies/?categories=1&location=all&publication_time=${conf.publicationTime}&limit=$limit&offset=$offset"
  }
}
