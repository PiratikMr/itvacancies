package org.example.headhunter.config

import org.example.core.config.loader.ArgsLoader
import org.example.core.config.loader.module.WithCommonArgsConfig
import org.rogach.scallop.ScallopOption

class HHArgsLoader(args: Array[String])
  extends ArgsLoader(args)
    with WithCommonArgsConfig
{
  private val dateFromOption: ScallopOption[String] = opt[String](name = "datefrom")
  lazy val dateFrom: String = dateFromOption()

  verify()
}
