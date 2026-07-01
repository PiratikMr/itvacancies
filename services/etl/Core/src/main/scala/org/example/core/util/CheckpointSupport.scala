package org.example.core.util

import org.apache.spark.sql.Dataset

object CheckpointSupport {

  implicit class DatasetCheckpointOps[T](private val ds: Dataset[T]) extends AnyVal {

    def reliableCheckpoint(): Dataset[T] = {
      val master = ds.sparkSession.sparkContext.getConf.get("spark.master", "")
      if (master.startsWith("local")) ds.localCheckpoint() else ds.checkpoint()
    }
  }
}
