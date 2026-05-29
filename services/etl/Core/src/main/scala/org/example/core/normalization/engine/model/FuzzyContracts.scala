package org.example.core.normalization.engine.model

case class FuzzyCandidate(
                           entityId: String,
                           rawValue: String,
                           parentId: Long
                         )

case class FuzzyDictionary(
                            dictId: Long,
                            normValue: String,
                            parentId: Long
                          )

case class FuzzyMatch(
                       entityId: String,
                       dictId: Long
                     )

case class FuzzyToCreate(
                          entityId: String,
                          rawValue: String,
                          parentId: Long
                        )

case class FuzzyMappingMeta(
                             normValue: String,
                             isCanonical: Boolean,
                             rawValue: String,
                             parentId: Long
                           )

object FuzzyColumns {
  val ENTITY_ID = "entityId"
  val RAW_VALUE = "rawValue"
  val PARENT_ID = "parentId"
  val NORM_VALUE = "normValue"
  val DICT_ID = "dictId"

  val IS_CANONICAL = "isCanonical"
}
