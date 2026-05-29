package org.example.core.config.database

class BridgeDef(val entity: String) {
  val vacancyId = "vacancy_id"
  val entityId = s"${entity}_id"

  val meta = TableMeta(
    s"bridge_vacancy_$entity",
    Seq(vacancyId, entityId)
  )
}
