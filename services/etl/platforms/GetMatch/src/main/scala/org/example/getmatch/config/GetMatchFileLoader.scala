package org.example.getmatch.config

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.module.{WithCommonFileConfig, WithStandardStructures}


class GetMatchFileLoader(confPath: String, override val saveFolder: String)
  extends FileLoader(confPath)
    with WithStandardStructures
    with WithCommonFileConfig {

  lazy val getMatch: GetMatchFileConfig = {
    val args = rootConfig.getConfig("Arguments")

    val vacsLimit = args.getInt("vacsLimit")
    val vacsPerPage = args.getInt("vacsPerPage")
    val inDays = args.getString("inDays")

    GetMatchFileConfig(vacsLimit, vacsPerPage, inDays)
  }
}