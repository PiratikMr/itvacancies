package org.example.headhunter

import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.adapter.storage.StorageAdapter
import org.example.core.adapter.web.WebAdapter
import org.example.headhunter.config.FolderNames
import org.example.headhunter.implement.{AreasTransformer, RolesTransformer}

object DictionaryLoader {

  def update(spark: SparkSession,
             webAdapter: WebAdapter,
             storageAdapter: StorageAdapter,
             apiBaseUrl: String): Unit = {

    val rawAreas = extract(spark, webAdapter, getUrl(apiBaseUrl, "/areas"))
    val areas = AreasTransformer.transform(spark, rawAreas)
    storageAdapter.write(areas, FolderNames.areas, withDate = false)

    val rawRoles = extract(spark, webAdapter, getUrl(apiBaseUrl, "/professional_roles"))
    val roles = RolesTransformer.transform(spark, rawRoles)
    storageAdapter.write(roles, FolderNames.roles, withDate = false)
  }


  private def extract(spark: SparkSession, webService: WebAdapter, url: String): Dataset[String] = {
    import spark.implicits._
    val body = webService.readBody(url).getOrElse("")
    Seq(body).toDS
  }

  private def getUrl(api: String, endPoint: String): String = s"$api$endPoint"
}
