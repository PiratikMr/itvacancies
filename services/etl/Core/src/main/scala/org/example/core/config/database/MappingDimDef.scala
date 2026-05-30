package org.example.core.config.database

class MappingDimDef(val entity: String) {
  val entityId = s"${entity}_id"
  val mappedValue = "mapped_value"
  val isCanonical = "is_canonical"

  val meta = TableMeta(
    s"mapping_dim_$entity",
    Seq(entityId, mappedValue)
  )

}
