package org.example.core.config.schema

import org.apache.spark.sql.types.{StructField, StructType}

trait Schema {
  protected val schemaFields: Seq[StructField]

  final lazy val schema: StructType = StructType(schemaFields)
}
