package org.example.core.normalization.factory

import org.apache.spark.sql.{Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.model.structures.FuzzyMatcherConf
import org.example.core.etl.model.Vacancy
import org.example.core.normalization.api.{BaseNormalizer, NormalizationCommand, NormalizationTask, TagExtractor}
import org.example.core.normalization.impl.{HierarchicalNormalizer, LanguageNormalizer, SimpleNormalizer}
import org.example.core.normalization.model.NormalizersEnum._
import org.example.core.normalization.service.NormalizeService

object NormalizerFactory {

  def createCommand(task: NormalizationTask, spark: SparkSession, dbAdapter: DataBaseAdapter, conf: FuzzyMatcherConf): NormalizationCommand = task match {

    case NormalizationTask.Standard(nType) =>
      val normalizer = getBaseNormalizer(nType, spark, dbAdapter, conf)
      (ds: Dataset[Vacancy]) => normalizer.process(ds, withCreate = true)

    case NormalizationTask.Exact(nType) =>
      val normalizer = getBaseNormalizer(nType, spark, dbAdapter, conf)
      (ds: Dataset[Vacancy]) => normalizer.process(ds, withCreate = false)

    case NormalizationTask.ExtractTags(nType, sourceCol) =>
      val extractor: TagExtractor = getSimpleNormalizer(nType, spark, dbAdapter, conf)
      (ds: Dataset[Vacancy]) => extractor.extractTags(ds, sourceCol)
  }

  private def getBaseNormalizer(nType: NormalizerType, spark: SparkSession, dbAdapter: DataBaseAdapter, conf: FuzzyMatcherConf): BaseNormalizer = {
    nType match {
      case simple: GroupNonHierarchical => getSimpleNormalizer(simple, spark, dbAdapter, conf)
      case LOCATIONS => new HierarchicalNormalizer(spark, dbAdapter, conf.get(LOCATIONS), conf.get(COUNTRY))
      case LANGUAGES => new LanguageNormalizer(spark, dbAdapter, conf.get(LANGUAGES), conf.get(LANGUAGES_LEVEL))
    }
  }

  private def getSimpleNormalizer(simple: GroupNonHierarchical, spark: SparkSession, dbAdapter: DataBaseAdapter, conf: FuzzyMatcherConf): SimpleNormalizer = {
    val coreService = new NormalizeService(
      spark,
      dbAdapter,
      conf.get(simple),
      simple.dimDef,
      simple.mappingDef
    )

    new SimpleNormalizer(spark, coreService, simple)
  }
}