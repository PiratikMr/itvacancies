package org.example.currency.config

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.module.{WithCommonFileConfig, WithStandardStructures}


class CurrencyFileLoader(confPath: String, override val saveFolder: String)
  extends FileLoader(confPath)
    with WithStandardStructures
    with WithCommonFileConfig {

  lazy val apiKey: String = rootConfig.getString("Arguments.apiKey")
}
