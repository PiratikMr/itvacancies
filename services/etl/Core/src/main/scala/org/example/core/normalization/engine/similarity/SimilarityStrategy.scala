package org.example.core.normalization.engine.similarity

import org.apache.spark.sql.Column

trait SimilarityStrategy {

  /**
   * String -> String
   */
  def normalize(col: Column): Column

  /**
   * String -> struct(text, ngrams, numbers)
   */
  def buildFeatures(c: Column): Column

  /**
   * struct(text, ngrams, numbers) -> Double
   */
  def calculateScore(c1: Column, c2: Column): Column

}
