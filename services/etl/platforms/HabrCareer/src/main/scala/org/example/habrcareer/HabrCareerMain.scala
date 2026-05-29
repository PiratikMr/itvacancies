package org.example.habrcareer

import org.example.core.adapter.database.impl.postgres.PostgresAdapter
import org.example.core.adapter.storage.impl.hdfs.HDFSAdapter
import org.example.core.adapter.web.impl.sttp.STTPAdapter
import org.example.core.config.model.structures.SparkConf
import org.example.core.etl.ETLUService
import org.example.core.util.SparkJob
import org.example.habrcareer.config.{HabrArgsLoader, HabrFileLoader}
import org.example.habrcareer.implement.{HabrExtractor, HabrTransformer}

object HabrCareerMain extends App with SparkJob {

  private val argsConfig = new HabrArgsLoader(args)
  private val fileConfig = new HabrFileLoader(argsConfig.common.confFile, argsConfig.common.saveFolder)

  override def sparkConf: SparkConf = fileConfig.structures.sparkConf

  override def sparkName: String = s"HabrCareer_${argsConfig.common.etlPart}"

  private val dbAdapter = new PostgresAdapter(fileConfig.structures.dbConf)

  private val ec = new ETLUService(
    spark,
    dbAdapter,
    new HDFSAdapter(fileConfig.structures.fsConf),
    STTPAdapter(fileConfig.structures.netConf)
  )

  private val extractor = new HabrExtractor(
    fileConfig.common.apiBaseUrl,
    fileConfig.vacsPageLimit,
    fileConfig.vacsPerPage,
    fileConfig.structures.netConf.requestsPS,
    fileConfig.common.rawPartitions
  )

  private val transformer = new HabrTransformer(
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