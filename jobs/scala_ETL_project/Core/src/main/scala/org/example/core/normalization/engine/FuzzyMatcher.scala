package org.example.core.normalization.engine

import org.apache.spark.sql.expressions.Window.partitionBy
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.config.model.structures.FuzzyMatchSettings
import org.example.core.normalization.engine.FuzzyMatcher._
import org.example.core.normalization.engine.model.FuzzyColumns._
import org.example.core.normalization.engine.model._
import org.example.core.normalization.engine.similarity.SimilarityStrategy
import org.example.core.normalization.engine.similarity.impl.DefaultSimilarityStrategy

class FuzzyMatcher(
                    spark: SparkSession,
                    similarityStrategy: SimilarityStrategy,
                    minScore: Double
                  ) {

  import spark.implicits._

  private val tagExtractor = new BroadcastTagExtractor(spark, similarityStrategy)


  def extractTags(
                   candidatesDs: Dataset[FuzzyCandidate],
                   dictionaryDs: Dataset[FuzzyDictionary]
                 ): Dataset[FuzzyMatch] = {
    tagExtractor.extractExactTags(candidatesDs, dictionaryDs)
  }

  def execute(
               candidatesDs: Dataset[FuzzyCandidate],
               dictionaryDs: Dataset[FuzzyDictionary]
             ): FuzzyMatcherResult = {

    val candidatesDf = candidatesDs.filter(col(RAW_VALUE).isNotNull)
      .withColumn(NORM_VALUE, similarityStrategy.normalize(col(RAW_VALUE)))
      .select(ENTITY_ID, RAW_VALUE, PARENT_ID, NORM_VALUE)
      .localCheckpoint()

    val dictDf = dictionaryDs.toDF()
      .select(DICT_ID, NORM_VALUE, PARENT_ID)


    // === 1. Точные совпадения ===
    val exactMatches = candidatesDf.join(dictDf, Seq(NORM_VALUE, PARENT_ID))
      .select(ENTITY_ID, NORM_VALUE, DICT_ID)

    val candidatesForFuzzy = candidatesDf.join(exactMatches, Seq(ENTITY_ID, NORM_VALUE), "left_anti")
      .select(ENTITY_ID, RAW_VALUE, PARENT_ID, NORM_VALUE)
      .localCheckpoint()

    if (candidatesForFuzzy.isEmpty) {
      return emptyResult(exactMatches)
    }
    // === Точные совпадения ===


    // === 2. Векторизация ===
    val fuzzyCandidatesWithFeatures = candidatesForFuzzy.withColumn(NORM_STRUCT, similarityStrategy.buildFeatures(col(NORM_VALUE)))
      .select(ENTITY_ID, RAW_VALUE, PARENT_ID, NORM_VALUE, NORM_STRUCT)
      .localCheckpoint()

    val dictWithFeatures = dictDf.withColumn(NORM_STRUCT, similarityStrategy.buildFeatures(col(NORM_VALUE)))
      .select(DICT_ID, NORM_VALUE, PARENT_ID, NORM_STRUCT)
    // === Векторизация ===


    // === 3. Fuzzy matching со словарями ===
    val fuzzyDictMatches = matchDictionary(fuzzyCandidatesWithFeatures, dictWithFeatures)
      .select(ENTITY_ID, DICT_ID)
      .localCheckpoint()

    val allDictMatches = exactMatches.select(ENTITY_ID, DICT_ID)
      .unionByName(fuzzyDictMatches)
      .distinct()

    val remainingCandidates = {
      fuzzyCandidatesWithFeatures.join(fuzzyDictMatches, Seq(ENTITY_ID), "left_anti")
        .select(ENTITY_ID, RAW_VALUE, PARENT_ID, NORM_VALUE, NORM_STRUCT)
        .localCheckpoint()
    }

    if (remainingCandidates.isEmpty) {
      return emptyResult(allDictMatches)
    }
    // === Fuzzy matching со словарями ===


    // === 4. Self fuzzy matching ===
    val selfMatches = selfMatching(remainingCandidates)
      .select(RAW_VALUE, NORM_VALUE, IS_CANONICAL, ENTITY_ID, PARENT_ID)
      .localCheckpoint()


    val toCreate = selfMatches.select(ENTITY_ID, RAW_VALUE, PARENT_ID)
      .distinct()
      .as[FuzzyToCreate]

    val newMappingData = selfMatches.select(NORM_VALUE, IS_CANONICAL, RAW_VALUE, PARENT_ID)
      .distinct()
      .as[FuzzyMappingMeta]

    val matchedResult = allDictMatches.as[FuzzyMatch]
    // === Self fuzzy matching ===

    FuzzyMatcherResult(matchedResult, toCreate, newMappingData, () => {})
  }


  private def matchDictionary(candidates: DataFrame, dictionaries: DataFrame): DataFrame = {
    candidates.alias("c").join(dictionaries.alias("d"),
        $"c.$PARENT_ID" === $"d.$PARENT_ID" &&
          arrays_overlap($"c.$NORM_STRUCT.ngrams", $"d.$NORM_STRUCT.ngrams")
      )
      .withColumn("score", similarityStrategy.calculateScore($"c.$NORM_STRUCT", $"d.$NORM_STRUCT"))
      .filter($"score" >= minScore)
      .withColumn("rank",
        row_number().over(
          partitionBy($"c.$NORM_VALUE", $"c.$ENTITY_ID", $"c.$PARENT_ID")
            .orderBy($"score".desc, $"d.$NORM_VALUE")
        )
      )
      .filter($"rank" === 1)
      .select(
        $"c.$ENTITY_ID",
        $"d.$DICT_ID"
      )
      .distinct()
  }

  private def selfMatching(candidatesDf: DataFrame): DataFrame = {
    val uniqueNorms = candidatesDf
      .select(NORM_VALUE, NORM_STRUCT, PARENT_ID)
      .distinct()
      .localCheckpoint()

    val normPairs   = buildNormPairs(uniqueNorms)
    val entityPairs = expandToEntityPairs(normPairs, candidatesDf).localCheckpoint()
    val ranked      = rankByAuthority(entityPairs).localCheckpoint()
    filterAndFinalRank(ranked)
  }

  private def buildNormPairs(uniqueNorms: DataFrame): DataFrame = {
    val uA = uniqueNorms.select(
      col(NORM_VALUE).as(A_NORM),
      col(NORM_STRUCT).as(A_STRUCT),
      col(PARENT_ID).as(A_PID)
    )
    val uB = uniqueNorms.select(
      col(NORM_VALUE).as(B_NORM),
      col(NORM_STRUCT).as(B_STRUCT),
      col(PARENT_ID).as(B_PID)
    )

    val lenA = length(col(A_NORM))
    val lenB = length(col(B_NORM))

    val halfPairs = uA
      .join(uB,
        col(A_PID) === col(B_PID) &&
          col(A_NORM) < col(B_NORM) &&
          abs(lenA - lenB) <= greatest(lenA, lenB) * lit(0.5) &&
          arrays_overlap($"$A_STRUCT.ngrams", $"$B_STRUCT.ngrams")
      )
      .withColumn("score", similarityStrategy.calculateScore(col(A_STRUCT), col(B_STRUCT)))
      .filter($"score" >= minScore)
      .select(col(A_NORM), col(B_NORM), col(A_PID), $"score")

    val flippedPairs = halfPairs.select(
      col(B_NORM).as(A_NORM),
      col(A_NORM).as(B_NORM),
      col(A_PID),
      $"score"
    )

    val selfPairs = uniqueNorms.select(
      col(NORM_VALUE).as(A_NORM),
      col(NORM_VALUE).as(B_NORM),
      col(PARENT_ID).as(A_PID),
      lit(1.0).as("score")
    )

    halfPairs.unionByName(flippedPairs).unionByName(selfPairs).cache()
  }

  private def expandToEntityPairs(normPairs: DataFrame, candidatesDf: DataFrame): DataFrame = {
    val A = candidatesDf.select(
      col(RAW_VALUE).as(A_RAW),
      col(NORM_VALUE).as("u_aN"),
      col(PARENT_ID).as("u_aPid")
    )
    val B = candidatesDf.select(
      col(ENTITY_ID).as(B_ID),
      col(NORM_VALUE).as("u_bN"),
      col(PARENT_ID).as("u_bPid")
    )

    normPairs
      .join(A, col(A_NORM) === col("u_aN") && col(A_PID) === col("u_aPid"))
      .join(B, col(B_NORM) === col("u_bN") && col(A_PID) === col("u_bPid"))
      .select(
        col(A_RAW), col(A_NORM), col(A_PID),
        col(B_ID), col(B_NORM), col("u_bPid").as(B_PID),
        col("score")
      )
  }

  private def rankByAuthority(pairs: DataFrame): DataFrame = {
    val authority = pairs
      .dropDuplicates(A_NORM, B_NORM)
      .groupBy(col(A_NORM).as("auth_norm"), col(A_PID).as("auth_parent"))
      .agg(sum("score").as("auth_score"))

    pairs
      .join(authority,
        col(A_NORM) === col("auth_norm") &&
          col(A_PID) === col("auth_parent")
      )
      .withColumn("rank1",
        row_number().over(
          partitionBy(B_NORM, A_PID)
            .orderBy(col("auth_score").desc, length(col(A_RAW)).asc, col(A_NORM).asc)
        )
      )
  }

  private def filterAndFinalRank(rankedCandidates: DataFrame): DataFrame = {
    val activeHubs = rankedCandidates
      .filter(col("rank1") === 1)
      .filter(col(A_NORM) =!= col(B_NORM))
      .select(col(A_NORM).as("hub"), col(A_PID).as("hub_pid"))
      .distinct()

    rankedCandidates
      .join(activeHubs,
        col("hub") === col(B_NORM) &&
          col(A_NORM) =!= col(B_NORM) &&
          col(A_PID) === col("hub_pid"),
        "left_anti"
      )
      .withColumn("rank2",
        row_number().over(
          partitionBy(B_ID, B_NORM, A_PID)
            .orderBy(col("rank1").asc)
        )
      )
      .filter(col("rank2") === 1)
      .withColumn(IS_CANONICAL, col(A_NORM) === col(B_NORM))
      .select(
        col(A_RAW).as(RAW_VALUE),
        col(B_NORM).as(NORM_VALUE),
        col(IS_CANONICAL),
        col(B_ID).as(ENTITY_ID),
        col(A_PID).as(PARENT_ID)
      ).distinct()
  }

  private def emptyResult(matchedDf: DataFrame, cache: () => Unit = () => {}): FuzzyMatcherResult = {
    FuzzyMatcherResult(
      matched = matchedDf.as[FuzzyMatch],
      toCreate = spark.emptyDataset[FuzzyToCreate],
      mappingData = spark.emptyDataset[FuzzyMappingMeta],
      cache
    )
  }
}

object FuzzyMatcher {
  private val NORM_STRUCT = "normStruct"

  import org.example.core.normalization.engine.model.FuzzyColumns._
  private val A_RAW    = s"A_$RAW_VALUE"
  private val A_NORM   = s"A_$NORM_VALUE"
  private val A_STRUCT = s"A_$NORM_STRUCT"
  private val A_PID    = s"A_$PARENT_ID"
  private val B_ID     = s"B_$ENTITY_ID"
  private val B_NORM   = s"B_$NORM_VALUE"
  private val B_STRUCT = s"B_$NORM_STRUCT"
  private val B_PID    = s"B_$PARENT_ID"

  def apply(spark: SparkSession, settings: FuzzyMatchSettings): FuzzyMatcher = {
    new FuzzyMatcher(spark, new DefaultSimilarityStrategy(settings), settings.minScoreThreshold)
  }
}