package org.example.core.etl.impl

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.functions.{col, explode}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.database._
import org.example.core.etl.Loader
import org.example.core.etl.model.NormalizedVacancy
import org.example.core.etl.model.VacancyColumns._
import org.example.core.util.mapper.VacancyDBMapper

import scala.collection.parallel.CollectionConverters._

class VacancyLoader(dbAdapter: DataBaseAdapter) extends Loader with LazyLogging {

  private val factDef = FactVacancyDef

  override def load(spark: SparkSession, ds: Dataset[NormalizedVacancy]): Unit = {

    val factVacancy = VacancyDBMapper.toFactVacancyTable(spark, ds)
      .toDF()
      .drop(factDef.vacancyId)

    val returnIds = dbAdapter.saveWithReturn(
      spark, factVacancy, factDef.meta.tableName,
      returns = Seq(factDef.vacancyId, factDef.externalId),
      conflicts = factDef.meta.conflictKeys,
      updates = Some(Seq(factDef.closedAt))
    )

    val dfWithId = ds.toDF().join(
      returnIds,
      ds(EXTERNAL_ID) === returnIds(factDef.externalId)
    ).cache()


    val simpleBridges = Seq(
      (BridgeVacancyEmploymentDef, EMPLOYMENT_IDS),
      (BridgeVacancyFieldDef, FIELD_IDS),
      (BridgeVacancyGradeDef, GRADE_IDS),
      (BridgeVacancyLocationDef, LOCATIONS),
      (BridgeVacancyScheduleDef, SCHEDULE_IDS),
      (BridgeVacancySkillDef, SKILL_IDS)
    )

    simpleBridges.par.foreach { case (bridgeDef, arrayColName) =>
      loadBridgeHelper(dfWithId, bridgeDef, arrayColName)
    }


    val languagesToWrite = dfWithId
      .withColumn("lang", explode(col(LANGUAGES)))
      .select(
        col(factDef.vacancyId),
        col(s"lang.$LANGUAGE_ID").as(BridgeVacancyLanguageDef.entityId),
        col(s"lang.$LEVEL_ID").as(BridgeVacancyLanguageDef.languageLevelId)
      )
      .distinct()
      .cache()

    if (!languagesToWrite.isEmpty) {
      dbAdapter.save(
        languagesToWrite,
        BridgeVacancyLanguageDef.meta.tableName,
        BridgeVacancyLanguageDef.meta.conflictKeys
      )
    }

    languagesToWrite.unpersist(blocking = false)
    dfWithId.unpersist(blocking = false)
  }


  private def loadBridgeHelper(df: DataFrame, bridge: BridgeDef, arrayColName: String): Unit = {
    val toWrite = df
      .withColumn(bridge.entityId, explode(col(arrayColName)))
      .select(
        col(factDef.vacancyId),
        col(bridge.entityId)
      )
      .distinct()
      .cache()

    dbAdapter.save(toWrite, bridge.meta.tableName, bridge.meta.conflictKeys)

    toWrite.unpersist(blocking = false)
  }

}