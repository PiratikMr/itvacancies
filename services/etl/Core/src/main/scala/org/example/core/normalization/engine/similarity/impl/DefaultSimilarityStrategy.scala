package org.example.core.normalization.engine.similarity.impl

import org.apache.spark.sql.Column
import org.apache.spark.sql.functions._
import org.example.core.config.model.structures.FuzzyMatchSettings
import org.example.core.normalization.engine.similarity.SimilarityStrategy

class DefaultSimilarityStrategy(settings: FuzzyMatchSettings) extends SimilarityStrategy {

  override def normalize(c: Column): Column = {
    val textTokens = TextNormalizer.normalize(c)
    array_join(array_sort(textTokens), " ")
  }

  override def buildFeatures(c: Column): Column = {
    struct(
      c.as("text"),
      array_distinct(ScoringAlgorithms.buildNGrams(c, settings.ngramSize)).as("ngrams"),
      ScoringAlgorithms.extractNumbers(c).as("numbers")
    )
  }

  override def calculateScore(s1: Column, s2: Column): Column = {
    val t1 = s1.getField("text")
    val t2 = s2.getField("text")

    val g1 = s1.getField("ngrams")
    val g2 = s2.getField("ngrams")

    val n1 = s1.getField("numbers")
    val n2 = s2.getField("numbers")

    val levScore = ScoringAlgorithms.levenshteinScore(t1, t2)
    val ngramScore = ScoringAlgorithms.nGramScoreFromArrays(g1, g2)
    val rawTextScore = (levScore * settings.levenshteinWeight) + (ngramScore * settings.ngramWeight)

    val numberFactor = ScoringAlgorithms.numberFactorFromArrays(n1, n2)
    val scoreWithPenalty = rawTextScore + (rawTextScore * numberFactor * settings.numberPenalty)

    clamp(scoreWithPenalty)
  }

  private def clamp(score: Column): Column = {
    least(lit(1.0), greatest(lit(0.0), score))
  }
}
