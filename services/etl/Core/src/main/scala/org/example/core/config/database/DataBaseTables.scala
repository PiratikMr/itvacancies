package org.example.core.config.database

import java.sql.Timestamp

// --- Dimension Definitions ---
case object DimCurrencyDef extends DimDef("currency")

object DimCurrencyRateHistoryDef {
  val currencyId: String = DimCurrencyDef.entityId
  val updateDate = "update_date"
  val rate = "rate"

  val meta: TableMeta = TableMeta(
    "dim_currency_rate_history",
    Seq(currencyId, updateDate)
  )
}

case object DimEmployerDef extends DimDef("employer")

case object DimEmploymentDef extends DimDef("employment")

case object DimExperienceDef extends DimDef("experience")

case object DimFieldDef extends DimDef("field")

case object DimGradeDef extends DimDef("grade")

case object DimLanguageLevelDef extends DimDef("language_level")

case object DimLanguageDef extends DimDef("language")

case object DimCountryDef extends DimDef("country") {
  val iso = "iso"
}

case object DimLocationDef extends DimDef("location", Some("country_id"))

case object DimPlatformDef extends DimDef("platform")

case object DimScheduleDef extends DimDef("schedule")

case object DimSkillDef extends DimDef("skill")

// --- Mapping Definitions ---
case object MappingCurrencyDef extends MappingDimDef("currency")

case object MappingEmployerDef extends MappingDimDef("employer")

case object MappingEmploymentDef extends MappingDimDef("employment")

case object MappingExperienceDef extends MappingDimDef("experience")

case object MappingFieldDef extends MappingDimDef("field")

case object MappingGradeDef extends MappingDimDef("grade")

case object MappingLanguageLevelDef extends MappingDimDef("language_level")

case object MappingLanguageDef extends MappingDimDef("language")

case object MappingCountryDef extends MappingDimDef("country")

case object MappingLocationDef extends MappingDimDef("location")

case object MappingPlatformDef extends MappingDimDef("platform")

case object MappingScheduleDef extends MappingDimDef("schedule")

case object MappingSkillDef extends MappingDimDef("skill")

// --- Bridge Definitions ---
case object BridgeVacancySkillDef extends BridgeDef("skill")

case object BridgeVacancyScheduleDef extends BridgeDef("schedule")

case object BridgeVacancyLocationDef extends BridgeDef("location")

case object BridgeVacancyFieldDef extends BridgeDef("field")

case object BridgeVacancyGradeDef extends BridgeDef("grade")

case object BridgeVacancyEmploymentDef extends BridgeDef("employment")


case object BridgeVacancyLanguageDef extends BridgeDef("language") {
  val languageLevelId = "language_level_id"

  override val meta: TableMeta = TableMeta(
    s"bridge_vacancy_$entity",
    Seq(vacancyId, entityId, languageLevelId)
  )
}

object FactVacancyDef {

  val vacancyId = "vacancy_id"
  val externalId = "external_id"
  val platformId = "platform_id"
  val employerId = "employer_id"
  val currencyId = "currency_id"
  val experienceId = "experience_id"
  val latitude = "latitude"
  val longitude = "longitude"
  val salaryFrom = "salary_from"
  val salaryTo = "salary_to"
  val publishedAt = "published_at"
  val title = "title"
  val url = "url"

  val closedAt = "closed_at"
  val updatedAt = "updated_at"

  val meta: TableMeta = TableMeta(
    "fact_vacancy",
    Seq(externalId, platformId)
  )

  case class Record(
                     vacancy_id: Option[Long] = None,
                     external_id: String,
                     title: String,
                     url: String,
                     platform_id: Long,
                     employer_id: Option[Long],
                     currency_id: Option[Long],
                     experience_id: Option[Long],
                     latitude: Option[Double],
                     longitude: Option[Double],
                     salary_from: Option[Double],
                     salary_to: Option[Double],
                     published_at: Timestamp
                   )
}

