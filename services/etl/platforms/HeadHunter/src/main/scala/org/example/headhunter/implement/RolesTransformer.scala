package org.example.headhunter.implement

import org.apache.spark.sql.functions.{col, explode}
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

object RolesTransformer {

  def transform(spark: SparkSession, rawDS: Dataset[String]): DataFrame =
    {
      val rawDF = spark.read.json(rawDS)
        .withColumn("c", explode(col("categories")))
        .select("c.*")

      rawDF
        .withColumn("r", explode(col("roles")))
        .select(
          col("id").cast(LongType).as("field_id"),
          col("r.id").cast(LongType).as("role_id"),
          col("r.name").as("name")
        )
        .dropDuplicates("field_id", "role_id")
    }
}
