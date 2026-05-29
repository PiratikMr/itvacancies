package org.example.core.util

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.SparkSession
import org.example.core.config.model.structures.SparkConf

trait SparkJob extends LazyLogging {

  def sparkConf: SparkConf

  def sparkName: String

  lazy val spark: SparkSession = {

    logger.info(s"Запуск Spark Session $sparkName...")

    val s = SparkSession.builder()
      .appName(s"${sparkConf.name}_$sparkName")
      .master(sparkConf.master)

      .config("spark.driver.memory", sparkConf.driverMemory)
      .config("spark.driver.cores", sparkConf.driverCores)
      .config("spark.executor.memory", sparkConf.executorMemory)
      .config("spark.executor.cores", sparkConf.executorCores)

      .getOrCreate()

    sys.addShutdownHook {
      logger.info(s"Остановка Spark Session $sparkName...")
      s.stop()
    }

    s
  }
}
