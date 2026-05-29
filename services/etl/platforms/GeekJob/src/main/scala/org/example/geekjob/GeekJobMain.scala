package org.example.geekjob

import org.example.core.adapter.database.impl.postgres.PostgresAdapter
import org.example.core.adapter.storage.impl.hdfs.HDFSAdapter
import org.example.core.adapter.web.impl.sttp.STTPAdapter
import org.example.core.adapter.web.impl.sttp.model.BackendType.UnsafeSSL
import org.example.core.config.model.structures.SparkConf
import org.example.core.etl.ETLUService
import org.example.core.util.SparkJob
import org.example.geekjob.config.{GeekJobArgsLoader, GeekJobFileLoader}
import org.example.geekjob.implement.{GeekJobExtractor, GeekJobTransformer}

object GeekJobMain extends App with SparkJob {

  private val argsConfig = new GeekJobArgsLoader(args)
  private val fileConfig = new GeekJobFileLoader(argsConfig.common.confFile, argsConfig.common.saveFolder)

  override def sparkConf: SparkConf = fileConfig.structures.sparkConf

  override def sparkName: String = argsConfig.common.etlPart


  private val dbAdapter = new PostgresAdapter(fileConfig.structures.dbConf)

  private val ec = new ETLUService(
    spark,
    dbAdapter,
    new HDFSAdapter(fileConfig.structures.fsConf),
    STTPAdapter(fileConfig.structures.netConf, UnsafeSSL)
  )

  private val extractor = new GeekJobExtractor(
    fileConfig.common.apiBaseUrl,
    fileConfig.pageLimit,
    fileConfig.structures.netConf.requestsPS,
    fileConfig.common.rawPartitions
  )

  private val transformer = new GeekJobTransformer(
    argsConfig.common.saveFolder,
    dbAdapter,
    fileConfig.structures.fuzzyMatcherConf
  )

  ec.run(
    argsConfig.common.etlPart,
    extractor = extractor,
    transformer = transformer,
    updateLimit = Some(fileConfig.common.updateLimit),
    platformName = fileConfig.structures.fsConf.platform
  )
}
