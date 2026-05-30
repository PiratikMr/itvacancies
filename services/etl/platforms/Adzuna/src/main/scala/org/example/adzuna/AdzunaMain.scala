package org.example.adzuna

import org.example.adzuna.config.{AdzunaArgsLoader, AdzunaFileLoader}
import org.example.adzuna.implement.{AdzunaExtractor, AdzunaTransformer}
import org.example.core.adapter.database.impl.postgres.PostgresAdapter
import org.example.core.adapter.storage.impl.hdfs.HDFSAdapter
import org.example.core.adapter.web.impl.sttp.STTPAdapter
import org.example.core.config.model.structures.SparkConf
import org.example.core.etl.ETLUService
import org.example.core.util.SparkJob

object AdzunaMain extends App with SparkJob {

  private val argsConfig = new AdzunaArgsLoader(args)
  private val fileConfig = new AdzunaFileLoader(argsConfig.common.confFile, argsConfig.common.saveFolder, argsConfig.locationIndex())

  override def sparkConf: SparkConf = fileConfig.structures.sparkConf

  override def sparkName: String = s"Adzuna_${argsConfig.common.etlPart}"

  private val dbAdapter = new PostgresAdapter(fileConfig.structures.dbConf)

  private val ec = new ETLUService(
    spark,
    dbAdapter,
    new HDFSAdapter(fileConfig.structures.fsConf),
    STTPAdapter(fileConfig.structures.netConf)
  )

  private val extractor = new AdzunaExtractor(
    fileConfig.common.apiBaseUrl, fileConfig.apiParams, fileConfig.pageLimit,
    fileConfig.structures.netConf.requestsPS, fileConfig.common.rawPartitions
  )

  private val transformer = new AdzunaTransformer(
    dbAdapter,
    fileConfig.structures.fuzzyMatcherConf,
    fileConfig.currency, fileConfig.urlDomain
  )

  ec.run(
    argsConfig.common.etlPart,
    extractor = extractor,
    transformer = transformer,
    platformName = fileConfig.structures.fsConf.platform,
    folderName = s"Vacancies/${fileConfig.apiParams.locationTag}"
  )
}
