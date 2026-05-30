package org.example.core.normalization.model

import org.example.core.config.database._
import org.example.core.etl.model.VacancyColumns

object NormalizersEnum {

  sealed abstract class NormalizerType(
                                        val mappedIdCol: String,
                                        val sourceCol: String,
                                        val isArray: Boolean
                                      )

  sealed abstract class GroupNonHierarchical(
                                              mappedIdCol: String,
                                              sourceCol: String,
                                              isArray: Boolean,
                                              val dimDef: DimDef,
                                              val mappingDef: MappingDimDef
                                            ) extends NormalizerType(mappedIdCol, sourceCol, isArray)

  sealed abstract class GroupHierarchical(
                                           mappedIdCol: String,
                                           sourceCol: String,
                                           isArray: Boolean
                                         ) extends NormalizerType(mappedIdCol, sourceCol, isArray)

  case object CURRENCY extends GroupNonHierarchical(FactVacancyDef.currencyId, VacancyColumns.CURRENCY, false, DimCurrencyDef, MappingCurrencyDef)

  case object EMPLOYER extends GroupNonHierarchical(FactVacancyDef.employerId, VacancyColumns.EMPLOYER, false, DimEmployerDef, MappingEmployerDef)

  case object EXPERIENCE extends GroupNonHierarchical(FactVacancyDef.experienceId, VacancyColumns.EXPERIENCE, false, DimExperienceDef, MappingExperienceDef)

  case object PLATFORM extends GroupNonHierarchical(FactVacancyDef.platformId, VacancyColumns.PLATFORM, false, DimPlatformDef, MappingPlatformDef)

  case object EMPLOYMENTS extends GroupNonHierarchical(BridgeVacancyEmploymentDef.entityId, VacancyColumns.EMPLOYMENTS, true, DimEmploymentDef, MappingEmploymentDef)

  case object FIELDS extends GroupNonHierarchical(BridgeVacancyFieldDef.entityId, VacancyColumns.FIELDS, true, DimFieldDef, MappingFieldDef)

  case object GRADES extends GroupNonHierarchical(BridgeVacancyGradeDef.entityId, VacancyColumns.GRADES, true, DimGradeDef, MappingGradeDef)

  case object SCHEDULES extends GroupNonHierarchical(BridgeVacancyScheduleDef.entityId, VacancyColumns.SCHEDULES, true, DimScheduleDef, MappingScheduleDef)

  case object SKILLS extends GroupNonHierarchical(BridgeVacancySkillDef.entityId, VacancyColumns.SKILLS, true, DimSkillDef, MappingSkillDef)

  case object COUNTRY extends GroupNonHierarchical(DimCountryDef.entityId, "", false, DimCountryDef, MappingCountryDef)

  case object LOCATIONS extends GroupHierarchical(BridgeVacancyLocationDef.entityId, VacancyColumns.LOCATIONS, true)

  case object LANGUAGES extends GroupHierarchical("mapped_languages", VacancyColumns.LANGUAGES, true)

  case object LANGUAGES_LEVEL extends GroupHierarchical(BridgeVacancyLanguageDef.languageLevelId, "", false)

  val values: Set[NormalizerType] = Set(
    CURRENCY, EMPLOYER, EXPERIENCE, PLATFORM, EMPLOYMENTS,
    FIELDS, GRADES, SCHEDULES, SKILLS, COUNTRY, LOCATIONS, LANGUAGES,
    LANGUAGES_LEVEL
  )
}