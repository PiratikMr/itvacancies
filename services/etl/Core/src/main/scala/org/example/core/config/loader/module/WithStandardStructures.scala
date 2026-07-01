package org.example.core.config.loader.module

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.parsing.StructuresFileParsing
import org.example.core.config.model.structures.StructuresConfig

import java.nio.file.Paths

trait WithStandardStructures {
  self: FileLoader[_] =>

  val saveFolder: String

  lazy val structures: StructuresConfig = {
    val fsConf = StructuresFileParsing.parseFSConf(rootConfig.getConfig("FileSystem"), saveFolder)

    StructuresConfig(
      StructuresFileParsing.parseDBConf(rootConfig.getConfig("DataBase")),
      fsConf,
      StructuresFileParsing.parseSparkConf(rootConfig.getConfig("Spark"), fsConf.checkpointPath),
      StructuresFileParsing.parseNetworkConf(rootConfig.getConfig("Network")),
      StructuresFileParsing.parseFuzzyMatcherConf(rootConfig.getConfig("FuzzyMatcher"))
    )
  }
}
