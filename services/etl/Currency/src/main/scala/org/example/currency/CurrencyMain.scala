package org.example.currency

import org.example.core.adapter.database.impl.postgres.PostgresAdapter
import org.example.core.adapter.storage.impl.hdfs.HDFSAdapter
import org.example.core.adapter.web.impl.sttp.STTPAdapter
import org.example.core.config.model.structures.SparkConf
import org.example.core.normalization.model.NormalizersEnum
import org.example.core.util.SparkJob
import org.example.currency.config.{CurrencyArgsLoader, CurrencyFileLoader}

object CurrencyMain extends App with SparkJob {

  private val argsConfig = new CurrencyArgsLoader(args)
  private val fileConfig = new CurrencyFileLoader(argsConfig.common.confFile, argsConfig.common.saveFolder)

  override def sparkConf: SparkConf = fileConfig.structures.sparkConf

  override def sparkName: String = "Currency"

  private val etlService = new ETLCurrency(
    spark = spark,
    dbAdapter = new PostgresAdapter(fileConfig.structures.dbConf),
    storageAdapter = new HDFSAdapter(fileConfig.structures.fsConf),
    webAdapter = STTPAdapter(fileConfig.structures.netConf),
    apiBaseUrl = fileConfig.common.apiBaseUrl,
    apiKey = fileConfig.apiKey,
    fuzzySettings = fileConfig.structures.fuzzyMatcherConf.get(NormalizersEnum.CURRENCY)
  )

  etlService.run(argsConfig.common.etlPart)
}
