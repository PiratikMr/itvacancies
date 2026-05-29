package org.example.core.config.model.structures

case class FuzzyMatchSettings(
                               minScoreThreshold: Double,

                               numberPenalty: Double,
                               ngramWeight: Double,
                               levenshteinWeight: Double,
                               ngramSize: Int
                             )
