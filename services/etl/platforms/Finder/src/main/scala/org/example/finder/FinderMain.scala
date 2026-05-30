package org.example.finder

import org.example.core.adapter.database.impl.postgres.PostgresAdapter
import org.example.core.adapter.storage.impl.hdfs.HDFSAdapter
import org.example.core.adapter.web.impl.sttp.STTPAdapter
import org.example.core.config.model.structures.SparkConf
import org.example.core.etl.ETLUService
import org.example.core.util.SparkJob
import org.example.finder.config.{FinderArgsLoader, FinderFileLoader}
import org.example.finder.implement.{FinderExtractor, FinderTransformer}

object FinderMain extends App with SparkJob {

  private val argsConfig = new FinderArgsLoader(args)
  private val fileConfig = new FinderFileLoader(argsConfig.common.confFile, argsConfig.common.saveFolder)

  override def sparkConf: SparkConf = fileConfig.structures.sparkConf

  override def sparkName: String = s"Finder_${argsConfig.common.etlPart}"


  private val dbAdapter = new PostgresAdapter(fileConfig.structures.dbConf)

  private val pc = new ETLUService(
    spark,
    dbAdapter,
    new HDFSAdapter(fileConfig.structures.fsConf),
    STTPAdapter(fileConfig.structures.netConf)
  )

  private val extractor = new FinderExtractor(fileConfig.finder, fileConfig.common.apiBaseUrl,
    fileConfig.structures.netConf.requestsPS, fileConfig.common.rawPartitions)
  private val transformer = new FinderTransformer(dbAdapter, fileConfig.structures.fuzzyMatcherConf)

  pc.run(
    etlPart = argsConfig.common.etlPart,
    extractor = extractor,
    transformer = transformer,
    updateLimit = Some(fileConfig.common.updateLimit),
    platformName = fileConfig.structures.fsConf.platform
  )

}
