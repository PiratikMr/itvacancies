package org.example.core.config.loader.parsing

import com.typesafe.config.Config
import org.example.core.config.model.structures._
import org.example.core.normalization.model.NormalizersEnum

import scala.jdk.CollectionConverters._

object StructuresFileParsing {

  def parseFSConf(config: Config, saveFolder: String): FSConf = {
    FSConf(
      url = config.getString("url"),
      dateFolder = saveFolder,
      rootPath = config.getString("path"),
      platform = config.getString("platform")
    )
  }

  def parseDBConf(config: Config): DBConf = {
    DBConf(
      name = config.getString("userName"),
      pass = config.getString("userPassword"),
      host = config.getString("host"),
      port = config.getString("port"),
      base = config.getString("baseName"),
      maxPartitions = config.getInt("maxPartitions"),
      batchSize = config.getInt("batchSize")
    )
  }

  def parseSparkConf(config: Config): SparkConf = {
    SparkConf(
      name = config.getString("name"),
      master = config.getString("master"),
      driverMemory = config.getString("driverMemory"),
      driverCores = config.getString("driverCores"),
      executorMemory = config.getString("executorMemory"),
      executorCores = config.getString("executorCores")
    )
  }

  def parseNetworkConf(config: Config): NetworkConf = {
    NetworkConf(
      headers = config.getConfig("headers").entrySet().asScala.map { entry =>
        val key = entry.getKey
        val value = config.getConfig("headers").getString(key)
        key -> value
      }.toArray,
      requestsPS = config.getInt("requestsPerSecond"),
      timeout = config.getInt("timeout")
    )
  }

  def parseFuzzyMatcherConf(config: Config): FuzzyMatcherConf = {
    val defaultConfig = config.getConfig("default")

    val settingsMap = NormalizersEnum.values.toSeq.map { enumValue =>
      val configKey = enumValue.toString.toLowerCase

      val specificConfig = if (config.hasPath(configKey)) {
        config.getConfig(configKey).withFallback(defaultConfig)
      } else {
        defaultConfig
      }

      val settings = FuzzyMatchSettings(
        minScoreThreshold = specificConfig.getDouble("minScoreThreshold"),
        numberPenalty = specificConfig.getDouble("numberPenalty"),
        ngramWeight = specificConfig.getDouble("ngramWeight"),
        levenshteinWeight = specificConfig.getDouble("levenshteinWeight"),
        ngramSize = specificConfig.getInt("ngramSize")
      )

      enumValue -> settings
    }.toMap

    FuzzyMatcherConf(settingsMap)
  }
}
