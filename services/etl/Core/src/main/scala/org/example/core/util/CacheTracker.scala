package org.example.core.util

import org.apache.spark.sql.DataFrame

class CacheTracker {

  private var cached = Seq.empty[DataFrame]

  def register(df: DataFrame): DataFrame = {
    val c = df.cache()
    cached = c +: cached
    c
  }

  def clearAll(): Unit = {
    cached.foreach(_.unpersist(blocking = false))
    cached = Seq.empty[DataFrame]
  }

}
