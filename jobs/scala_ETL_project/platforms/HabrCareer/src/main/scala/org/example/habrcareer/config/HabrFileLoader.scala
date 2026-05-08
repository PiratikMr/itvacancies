package org.example.habrcareer.config

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.module.{WithCommonFileConfig, WithStandardStructures}

class HabrFileLoader(confPath: String, override val saveFolder: String)
  extends FileLoader(confPath)
    with WithStandardStructures
    with WithCommonFileConfig {

  private lazy val args = rootConfig.getConfig("Arguments")

  lazy val vacsPageLimit: Int = math.max(1, args.getInt("vacsPageLimit"))
  lazy val vacsPerPage: Int = args.getInt("vacsPerPage")
}
