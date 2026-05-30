package org.example.headhunter

import org.apache.spark.sql.functions.col
import org.example.core.adapter.database.impl.postgres.PostgresAdapter
import org.example.core.adapter.storage.impl.hdfs.HDFSAdapter
import org.example.core.adapter.web.impl.sttp.STTPAdapter
import org.example.core.config.model.structures.SparkConf
import org.example.core.etl.ETLUService
import org.example.core.util.SparkJob
import org.example.headhunter.config.{FolderNames, HHArgsLoader, HHFileLoader}
import org.example.headhunter.implement.{HHExtractor, HHTransformer}

import scala.util.Try

object HeadHunterMain extends App with SparkJob /*with LazyLogging*/ {

  private val argsConfig = new HHArgsLoader(args)
  private val fileConfig = new HHFileLoader(argsConfig.common.confFile, argsConfig.common.saveFolder)

  override def sparkConf: SparkConf = fileConfig.structures.sparkConf

  override def sparkName: String = s"HeadHunter_${argsConfig.common.etlPart}"

  private val dbService = new PostgresAdapter(fileConfig.structures.dbConf)
  private val hdfsAdapter = new HDFSAdapter(fileConfig.structures.fsConf)
  private val sttpAdapter = STTPAdapter(fileConfig.structures.netConf)

  import spark.implicits._


  private lazy val dictionariesMissingOrEmpty = Try {
    val areas = hdfsAdapter.read(spark, FolderNames.areas, withDate = false)
    val roles = hdfsAdapter.read(spark, FolderNames.roles, withDate = false)

    areas.isEmpty || roles.isEmpty
  }.getOrElse(true)

  if (fileConfig.forceDict || dictionariesMissingOrEmpty) {
    DictionaryLoader.update(spark, sttpAdapter, hdfsAdapter, fileConfig.common.apiBaseUrl)
  }


  private val profRolesIds = hdfsAdapter.read(spark, FolderNames.roles, withDate = false)
    .where(col("field_id").isin(fileConfig.fieldIDs: _*))
    .select("role_id")
    .map(row => row.getLong(0))

  private val ec = new ETLUService(
    spark,
    dbService,
    hdfsAdapter,
    sttpAdapter
  )

  private val extractor = new HHExtractor(
    profRolesIds,
    fileConfig.common.apiBaseUrl,
    argsConfig.dateFrom,
    fileConfig.pageLimit,
    fileConfig.vacsPerPage,
    fileConfig.structures.netConf.requestsPS,
    fileConfig.common.rawPartitions
  )

  private val areas = hdfsAdapter.read(spark, FolderNames.areas, withDate = false)
    .withColumnRenamed("id", "area_id")

  ec.run(
    argsConfig.common.etlPart,
    extractor = extractor,
    transformer = new HHTransformer(areas, dbService, fileConfig.structures.fuzzyMatcherConf),
    updateLimit = Some(fileConfig.common.updateLimit),
    platformName = fileConfig.structures.fsConf.platform
  )
}
