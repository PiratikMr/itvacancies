package org.example.core.normalization.impl

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.database.{DimLanguageDef, DimLanguageLevelDef, MappingLanguageDef, MappingLanguageLevelDef}
import org.example.core.config.model.structures.FuzzyMatchSettings
import org.example.core.etl.model.{Vacancy, VacancyColumns}
import org.example.core.normalization.api.BaseNormalizer
import org.example.core.normalization.impl.LanguageNormalizer._
import org.example.core.normalization.model.NormCandidate
import org.example.core.normalization.model.NormalizationColumns._
import org.example.core.normalization.service.NormalizeService

class LanguageNormalizer(spark: SparkSession,
                         dbAdapter: DataBaseAdapter,
                         languageSettings: FuzzyMatchSettings,
                         levelSettings: FuzzyMatchSettings) extends BaseNormalizer {

  import spark.implicits._

  private val languageNormalizeService = new NormalizeService(spark, dbAdapter, languageSettings, DimLanguageDef, MappingLanguageDef)
  private val levelNormalizeService = new NormalizeService(spark, dbAdapter, levelSettings, DimLanguageLevelDef, MappingLanguageLevelDef)

  override def process(vacancies: Dataset[Vacancy], withCreate: Boolean): DataFrame = {

    val rawData = vacancies.toDF()
      .select(
        col(VacancyColumns.EXTERNAL_ID),
        posexplode_outer(col(VacancyColumns.LANGUAGES)).as(Seq(POS, LANG_STRUCT))
      )
      .filter(col(LANG_STRUCT).isNotNull)
      .withColumn(UNIQUE_ID, concat(col(VacancyColumns.EXTERNAL_ID), lit("_"), col(POS)))
      .cache()

    val levelsData = rawData.select(
      col(UNIQUE_ID).as(ENTITY_ID),
      col(LANG_STRUCT).getField(VacancyColumns.LEVEL).cast("string").as(RAW_VALUE),
      lit(null).cast("string").as(PARENT_ID)
    ).filter(col(RAW_VALUE).isNotNull).as[NormCandidate]

    val nLevels = levelNormalizeService.mapSimple(levelsData, withCreate).toDF()
      .withColumnRenamed(ENTITY_ID, UNIQUE_ID)
      .withColumnRenamed(MAPPED_ID, "levelId")

    val langsData = rawData.select(
      col(UNIQUE_ID).as(ENTITY_ID),
      col(LANG_STRUCT).getField(VacancyColumns.LANGUAGE).cast("string").as(RAW_VALUE),
      lit(null).cast("string").as(PARENT_ID)
    ).filter(col(RAW_VALUE).isNotNull).as[NormCandidate]

    val nLangs = languageNormalizeService.mapSimple(langsData, withCreate).toDF()
      .withColumnRenamed(ENTITY_ID, UNIQUE_ID)
      .withColumnRenamed(MAPPED_ID, "languageId")

    val finalRes = rawData.select(col(VacancyColumns.EXTERNAL_ID).as(ENTITY_ID), col(UNIQUE_ID))
      .join(nLevels, Seq(UNIQUE_ID), "left")
      .join(nLangs, Seq(UNIQUE_ID), "inner")
      .select(
        col(ENTITY_ID),
        struct(
          col("languageId").cast("long").as(VacancyColumns.LANGUAGE_ID),
          col("levelId").cast("long").as(VacancyColumns.LEVEL_ID)
        ).as("mapped_lang_struct")
      )

    rawData.unpersist(blocking = false)

    finalRes
      .groupBy(ENTITY_ID)
      .agg(collect_list(col("mapped_lang_struct")).as("mapped_languages"))
  }
}

object LanguageNormalizer {
  private val POS = "pos"
  private val LANG_STRUCT = "lang_struct"
  private val UNIQUE_ID = "uniqueId"
}