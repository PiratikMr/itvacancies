package org.example.finder.config

import org.example.core.config.loader.FileLoader


trait WithFinderFileConfig {
  self: FileLoader[_] =>

  lazy val finder: FinderFileConfig = {
    val args = rootConfig.getConfig("Arguments")

    val vacsLimit: Int = args.getInt("vacsLimit")
    val vacsPerPage: Int = args.getInt("vacsPerPage")
    val publicationTime: String = args.getString("publicationTime")

    FinderFileConfig(vacsLimit, vacsPerPage, publicationTime)
  }
}
