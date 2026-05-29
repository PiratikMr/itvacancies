package org.example.headhunter.implement

import org.apache.spark.sql.functions.{col, explode, size}
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

import scala.annotation.tailrec

object AreasTransformer {

  def transform(spark: SparkSession, rawDS: Dataset[String]): DataFrame =
    {
      val rawDF = spark.read.json(rawDS)

      val rootDF = rawDF
        .withColumn("country", col("name"))
        .select(
          col("id").cast(LongType),
          col("name").as("region"),
          col("country"),
          col("areas")
        )

      @tailrec
      def flattenAreas(acc: DataFrame, current: DataFrame): DataFrame = {
        current.cache()

        val newAcc = acc.union(current.drop("areas"))

        val hasChildren = current.filter(size(col("areas")) > 0).count > 0

        if (!hasChildren) {
          current.unpersist()
          newAcc

        } else {
          val nextLevel = current
            .filter(size(col("areas")) > 0)
            .withColumn("child", explode(col("areas")))
            .select(
              col("child.id").cast(LongType),
              col("child.name").as("region"),
              col("country"),
              col("child.areas")
            )

          current.unpersist()
          flattenAreas(newAcc, nextLevel)
        }
      }

      val emptyAcc = rootDF.drop("areas").limit(0)

      flattenAreas(emptyAcc, rootDF)
        .select("id", "region", "country")
        .dropDuplicates("id")
    }
}
