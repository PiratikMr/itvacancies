package org.example.core.config.loader.module

import com.typesafe.config.Config
import org.example.core.config.loader.FileLoader
import org.example.core.config.model.CommonFileConfig

trait WithCommonFileConfig {

  self: FileLoader[_] =>

  lazy val common: CommonFileConfig = {
    val args: Config = rootConfig.getConfig("Arguments")

    val rawPartitions: Int = args.getInt("rawPartitions")
    val updateLimit: Int = args.getInt("updateLimit")

    val apiBaseUrl: String = args.getString("apiBaseUrl")

    CommonFileConfig(rawPartitions, updateLimit, apiBaseUrl)
  }
}