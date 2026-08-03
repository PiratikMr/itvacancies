package org.example.adzuna.config

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.module.{WithCommonFileConfig, WithStandardStructures}

class AdzunaFileLoader(confPath: String, override val saveFolder: String, locationIndex: Int)
  extends FileLoader(confPath)
    with WithStandardStructures
    with WithCommonFileConfig {

  private val args = rootConfig.getConfig("Arguments")

  private val keyIndex: Int = args.getIntList("appKeyIndexes").get(locationIndex)

  lazy val apiParams = AdzunaApiParams(
    args.getStringList("locationTags").get(locationIndex),
    args.getInt("maxDaysOld"),
    args.getInt("vacsPerPage"),
    credential("appIds"),
    credential("appKeys"),
    args.getString("categoryTag")
  )

  private def credential(listName: String): String = {
    val values = args.getStringList(listName)

    require(
      keyIndex < values.size,
      s"$listName: No value with index $keyIndex, length is  ${values.size}"
    )

    val value = values.get(keyIndex)
    require(value.nonEmpty, s"$listName[$keyIndex] is empty")

    value
  }

  lazy val currency: String = args.getStringList("currencies").get(locationIndex)
  lazy val urlDomain: String = args.getStringList("urlDomains").get(locationIndex)

  lazy val pageLimit: Int = args.getInt("pageLimit")

  lazy val expireAfterDays: Int = args.getInt("expireAfterDays")
}
