package org.example.core.config.loader.module

import org.example.core.config.loader.ArgsLoader
import org.example.core.config.model.CommonArgsConfig

trait WithCommonArgsConfig {
  self: ArgsLoader =>

  private val etlPart = opt[String]("etlpart", default = Some("unknown"))
  private val confFile = opt[String]("conffile", required = true)
  private val saveFolder = opt[String]("savefolder", default = Some("undefined"))

  lazy val common: CommonArgsConfig = {
    CommonArgsConfig(etlPart(), confFile(), saveFolder())
  }
}
