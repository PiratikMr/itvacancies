package org.example.core.normalization.engine.similarity.impl

import org.apache.spark.sql.Column
import org.apache.spark.sql.functions._

object ScoringAlgorithms {

  def buildNGrams(c: Column, n: Int): Column = {
    val len = length(c)
    when(c.isNull || len < lit(n), typedLit(Seq.empty[String]))
      .otherwise {
        val indices = sequence(lit(1), len - lit(n - 1))
        transform(indices, i => substring(c, i, lit(n)))
      }
  }

  def extractNumbers(c: Column): Column = {
    val onlyDigits = regexp_replace(c, "[^0-9]+", " ")
    val splitted = split(trim(onlyDigits), "\\s+")
    array_remove(splitted, "")
  }

  def levenshteinScore(v1: Column, v2: Column): Column = {
    val dist = levenshtein(v1, v2)
    val maxLen = greatest(length(v1), length(v2))

    when(maxLen === 0, lit(1.0))
      .otherwise(lit(1.0) - (dist / maxLen))
  }

  def nGramScoreFromArrays(v1: Column, v2: Column): Column = {
    val len1 = size(v1)
    val len2 = size(v2)

    when((len1 + len2) === 0, lit(0.0))
      .otherwise({
        val intersection = size(array_intersect(v1, v2))
        lit(2.0) * intersection / (len1 + len2)
      })
  }

  def numberFactorFromArrays(v1: Column, v2: Column): Column = {
    val s1 = size(v1)
    val s2 = size(v2)

    when((s1 + s2) === 0, lit(0.0))
      .otherwise({
        val intersection = size(array_intersect(v1, v2))
        val union = size(array_union(v1, v2))

        (lit(2.0) * intersection / union) - lit(1.0)
      })
  }
}
