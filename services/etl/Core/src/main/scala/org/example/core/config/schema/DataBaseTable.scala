package org.example.core.config.schema

import org.apache.spark.sql.types.{BooleanType, DoubleType, LongType, StringType, StructField}

abstract class DataBaseTable(val tableName: String) extends Schema

class DataBaseDimTable(entity: String, parentIdName: String = "")
  extends DataBaseTable(s"dim_$entity") {

  val entityId = StructField(s"${entity}_id", LongType, nullable = false)
  val name = StructField(entity, StringType, nullable = false)
  val parentId = StructField(parentIdName, LongType, nullable = false)

  override protected val schemaFields: Seq[StructField] = Seq(
    entityId,
    name,
    parentId
  )
}



class DataBaseMappingDimTable(entity: String)
  extends DataBaseTable(s"mapping_dim_$entity") {

  val entityId = StructField(s"${entity}_id", LongType, nullable = false)
  val mappedValue = StructField("mapped_value", StringType, nullable = false)
  val isCanonical = StructField("is_canonical", BooleanType, nullable = false)

  override protected val schemaFields: Seq[StructField] = Seq(
    entityId,
    mappedValue,
    isCanonical
  )
}

class DataBaseBridgeTable(entity: String)
  extends DataBaseTable(s"bridge_vacancy_$entity") {
  val vacancyId = StructField("vacancy_id", LongType, nullable = false)
  val entityId = StructField(s"${entity}_id", LongType, nullable = false)

  override protected val schemaFields: Seq[StructField] = Seq(
    vacancyId,
    entityId
  )
}


class DataBaseOneToManyEntity(entity: String) {
  val dimTable = new DataBaseDimTable(entity)
  val mappingDimTable = new DataBaseMappingDimTable(entity)
}

class DataBaseManyToManyEntity(entity: String) extends DataBaseOneToManyEntity(entity) {
  val bridge = new DataBaseBridgeTable(entity)
}
