package org.example.core.etl

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.functions.{coalesce, col, current_timestamp, lit}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.adapter.storage.StorageAdapter
import org.example.core.adapter.web.WebAdapter
import org.example.core.etl.impl.{VacancyLoader, VacancyUpdater}
import org.example.core.etl.model.ETLParts.{Extract, TransformLoad, Update}
import org.example.core.etl.model.{ETLParts, NormalizedVacancy, VacancyColumns}
import org.example.core.etl.utils.DataTransformer
import org.example.core.util.CheckpointSupport._

import scala.util.{Failure, Success}

class ETLUService(
                   spark: SparkSession,
                   dbAdapter: DataBaseAdapter,
                   storageAdapter: StorageAdapter,
                   webAdapter: WebAdapter
                 ) extends LazyLogging {


  private val vacancyUpdater = new VacancyUpdater(spark, dbAdapter)


  private def extract(extractor: Extractor, folderName: String): Unit = {
    val rawDS = extractor.extract(spark, webAdapter)
    storageAdapter.writeText(rawDS, folderName)
  }

  private def transform(transformer: Transformer, folderName: String): Dataset[NormalizedVacancy] = {
    val rawDS: Dataset[String] = storageAdapter.readText(spark, folderName)
    val rawDF: DataFrame = transformer.toRows(spark, rawDS)

    val transformedDs = transformer.transform(spark, rawDF)
      .dropDuplicates(VacancyColumns.EXTERNAL_ID)
      .reliableCheckpoint()

    val formattedSkillsDs = DataTransformer.normalizeSkills(transformedDs)

    val normalized = transformer.normalize(spark, formattedSkillsDs)
      .dropDuplicates(VacancyColumns.EXTERNAL_ID)
      .reliableCheckpoint()

    normalized
  }

  private def update(extractor: Extractor, updateLimit: Int, platformName: String, maxAgeDays: Option[Int]): Unit = {

    val activeIds = vacancyUpdater.getActiveVacancies(updateLimit, platformName, maxAgeDays)
      .reliableCheckpoint()

    val unActiveIds = extractor.filterActiveVacancies(spark, activeIds, webAdapter)
      .reliableCheckpoint()

    saveLiveness(activeIds, unActiveIds, platformName)

    vacancyUpdater.updateVacancies(unActiveIds, platformName)
  }


  private def saveLiveness(checkedIds: Dataset[String], unActiveIds: Dataset[String], platformName: String): Unit = {

    val unActiveDf = unActiveIds.toDF(VacancyColumns.EXTERNAL_ID)
      .dropDuplicates(VacancyColumns.EXTERNAL_ID)
      .withColumn(ETLUService.IS_ALIVE, lit(false))

    val livenessDf = checkedIds.toDF(VacancyColumns.EXTERNAL_ID)
      .join(unActiveDf, Seq(VacancyColumns.EXTERNAL_ID), "left")
      .withColumn(ETLUService.IS_ALIVE, coalesce(col(ETLUService.IS_ALIVE), lit(true)))
      .withColumn(VacancyColumns.PLATFORM, lit(platformName))
      .withColumn(ETLUService.CHECKED_AT, current_timestamp())

    storageAdapter.write(livenessDf, ETLUService.ALIVE_FOLDER, withDate = true)
  }


  def run(
           etlPart: String,
           extractor: Extractor,
           transformer: Transformer,
           loader: Option[Loader] = None,
           updateLimit: Option[Int] = None,
           updateMaxAgeDays: Option[Int] = None,
           platformName: String,
           folderName: String = "Vacancies"
         ): Unit = {

    ETLParts.parse(etlPart) match {
      case Success(Extract) =>
        extract(extractor, folderName)

      case Success(TransformLoad) =>
        val df = transform(transformer, folderName)
        loader.getOrElse(new VacancyLoader(dbAdapter)).load(spark, df)

      case Success(Update) =>
        updateLimit match {
          case Some(limit) => update(extractor, limit, platformName, updateMaxAgeDays)
          case None =>
            logger.error("Ошибка запуска ETL: Не передан updateLimit для процесса Update")
        }

      case Failure(e) =>

        logger.error(s"Ошибка запуска ETL: ${e.getMessage}")

        throw e
    }
  }
}

object ETLUService {

  private val ALIVE_FOLDER = "Alive"

  private val IS_ALIVE = "isAlive"
  private val CHECKED_AT = "checkedAt"
}
