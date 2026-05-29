package org.example.core.normalization.model

object NormalizationColumns {
  val ENTITY_ID = "entityId"
  val RAW_VALUE = "rawValue"
  val MAPPED_ID = "mappedId"
  val PARENT_ID = "parentId"
}

case class NormCandidate(
                          entityId: String,
                          rawValue: String,
                          parentId: Option[String]
                        )

case class NormMatch(
                      entityId: String,
                      mappedId: Long
                    )