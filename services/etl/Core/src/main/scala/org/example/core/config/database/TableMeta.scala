package org.example.core.config.database

case class TableMeta(tableName: String, conflictKeys: Seq[String])