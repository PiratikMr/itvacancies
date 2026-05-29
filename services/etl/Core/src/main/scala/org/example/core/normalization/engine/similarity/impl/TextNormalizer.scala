package org.example.core.normalization.engine.similarity.impl

import org.apache.spark.sql.Column
import org.apache.spark.sql.functions._

import java.util.regex.Pattern

object TextNormalizer {

  private val GarbagePattern = Pattern.compile("http[s]?://\\S+|<[^>]+>|&[a-z0-9#]+;|\\[(.*?)\\]\\([^)]+\\)")
  private val KeepPattern = Pattern.compile("[^a-z0-9а-я+#]")

  private val normalizeUdf = udf((text: String) => {
    if (text == null || text.trim.isEmpty) {
      Seq.empty[String]
    } else {
      var cleaned = text.toLowerCase.replace("ё", "е").replace("й", "и")

      cleaned = GarbagePattern.matcher(cleaned).replaceAll(" ")

      cleaned = KeepPattern.matcher(cleaned).replaceAll(" ")

      cleaned.split("\\s+").filter(_.nonEmpty).map { word =>
        val stemmed = StemmerProvider.stem(word)
        s"_${stemmed}_"
      }.toSeq
    }
  })

  def normalize(c: Column): Column = {
    normalizeUdf(c)
  }
}
