package org.example.headhunter.config

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.module.{WithCommonFileConfig, WithStandardStructures}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class HHFileLoader(confPath: String, override val saveFolder: String)
  extends FileLoader(confPath)
    with WithStandardStructures
    with WithCommonFileConfig
{

  private val args = rootConfig.getConfig("Arguments")

  lazy val fieldIDs: Seq[Int] = args.getIntList("fieldIds").asScala.map(_.toInt).toSeq
  lazy val vacsPerPage: Int = args.getInt("vacsPerPage")
  lazy val pageLimit: Int = args.getInt("pageLimit")

  lazy val forceDict: Boolean = args.getBoolean("forceDict")
}
