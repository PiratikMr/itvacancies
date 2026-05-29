package org.example.core.config.database

class DimDef(entity: String, val parentId: Option[String] = None) {
  val entityId = s"${entity}_id"
  val entityName: String = entity

  val meta = TableMeta(
    s"dim_$entity",
    Seq(entityName) ++ parentId.toSeq
  )
}