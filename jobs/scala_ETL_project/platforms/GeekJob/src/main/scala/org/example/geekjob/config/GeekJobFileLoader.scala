package org.example.geekjob.config

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.module.{WithCommonFileConfig, WithStandardStructures}

class GeekJobFileLoader(confPath: String, override val saveFolder: String)
  extends FileLoader(confPath)
    with WithStandardStructures
    with WithCommonFileConfig {

  lazy val pageLimit: Int = rootConfig.getInt("Arguments.pageLimit")
}