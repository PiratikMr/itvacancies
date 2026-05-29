package org.example.core.config.loader.module

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.parsing.StructuresFileParsing
import org.example.core.config.model.structures.StructuresConfig

import java.nio.file.Paths

trait WithStandardStructures {
  self: FileLoader[_] =>

  val saveFolder: String

  lazy val structures: StructuresConfig = {
    StructuresConfig(
      StructuresFileParsing.parseDBConf(rootConfig.getConfig("DataBase")),
      StructuresFileParsing.parseFSConf(rootConfig.getConfig("FileSystem"), saveFolder),
      StructuresFileParsing.parseSparkConf(rootConfig.getConfig("Spark")),
      StructuresFileParsing.parseNetworkConf(rootConfig.getConfig("Network")),
      StructuresFileParsing.parseFuzzyMatcherConf(rootConfig.getConfig("FuzzyMatcher"))
    )
  }
}
