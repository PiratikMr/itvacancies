package org.example.getmatch

import org.example.core.adapter.database.impl.postgres.PostgresAdapter
import org.example.core.adapter.storage.impl.hdfs.HDFSAdapter
import org.example.core.adapter.web.impl.sttp.STTPAdapter
import org.example.core.config.model.structures.SparkConf
import org.example.core.etl.ETLUService
import org.example.core.util.SparkJob
import org.example.getmatch.config.{GetMatchArgsLoader, GetMatchFileLoader}
import org.example.getmatch.implement.{GetMatchExtractor, GetMatchTransformer}

object GetMatchMain extends App with SparkJob {

  private val argsConfig = new GetMatchArgsLoader(args)
  private val fileConfig = new GetMatchFileLoader(argsConfig.common.confFile, argsConfig.common.saveFolder)

  override def sparkConf: SparkConf = fileConfig.structures.sparkConf

  override def sparkName: String = s"GetMatch_${argsConfig.common.etlPart}"

  private val dbAdapter = new PostgresAdapter(fileConfig.structures.dbConf)


  private val ec = new ETLUService(
    spark,
    dbAdapter,
    new HDFSAdapter(fileConfig.structures.fsConf),
    STTPAdapter(fileConfig.structures.netConf)
  )


  private val extractor = new GetMatchExtractor(
    fileConfig.getMatch,
    fileConfig.common.apiBaseUrl,
    fileConfig.structures.netConf.requestsPS,
    fileConfig.common.rawPartitions
  )

  private val transformer = new GetMatchTransformer(
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
