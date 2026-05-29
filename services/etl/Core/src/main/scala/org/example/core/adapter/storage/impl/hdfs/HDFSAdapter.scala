package org.example.core.adapter.storage.impl.hdfs

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode, SparkSession}
import org.example.core.adapter.storage.StorageAdapter
import org.example.core.config.model.structures.FSConf

import scala.util.control.NonFatal

class HDFSAdapter(conf: FSConf) extends StorageAdapter with LazyLogging {

  override def read(spark: SparkSession, folderName: String, withDate: Boolean = true): DataFrame = {
    val path = conf.getPath(folderName, withDate)

    logger.info(s"Чтение DataFrame (parquet) из HDFS по пути: $path")

    try {
      val df = spark.read.parquet(path)
      logger.debug(s"Успешно прочитан DataFrame из $path")
      df

    } catch {
      case NonFatal(e) =>
        logger.error(s"Ошибка при чтении DataFrame из $path", e)
        throw e
    }
  }

  override def readText(spark: SparkSession, folderName: String): Dataset[String] = {
    val path = conf.getPath(folderName)

    logger.info(s"Чтение текстового Dataset из HDFS по пути: $path")

    try {
      val ds = spark.read.option("multiLine","true").textFile(path)
      logger.debug(s"Успешно прочитан текстовый Dataset из $path")
      ds

    } catch {
      case NonFatal(e) =>
        logger.error(s"Ошибка при чтении текстового Dataset из $path", e)
        throw e
    }
  }

  override def write(df: DataFrame, folderName: String, withDate: Boolean = true): Unit = {
    val path = conf.getPath(folderName, withDate)

    logger.info(s"Запись DataFrame (parquet) в HDFS по пути: $path. SaveMode: Overwrite")

    try {
      df.write.mode(SaveMode.Overwrite).parquet(path)
      logger.debug(s"Успешная запись DataFrame в $path")

    } catch {
      case NonFatal(e) =>
        logger.error(s"Ошибка при записи DataFrame в $path", e)
        throw e
    }
  }

  override def writeText(ds: Dataset[String], folderName: String): Unit = {
    val path = conf.getPath(folderName)

    logger.info(s"Запись текстового Dataset в HDFS по пути: $path. SaveMode: Overwrite")

    try {
      ds.write.mode(SaveMode.Overwrite).text(path)
      logger.debug(s"Успешная запись текстового Dataset в $path")

    } catch {
      case NonFatal(e) =>
        logger.error(s"Ошибка при записи текстового Dataset в $path", e)
        throw e
    }
  }
}
