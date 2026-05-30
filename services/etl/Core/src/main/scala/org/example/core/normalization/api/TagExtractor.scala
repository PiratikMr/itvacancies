package org.example.core.normalization.api

import org.apache.spark.sql.{DataFrame, Dataset}
import org.example.core.etl.model.Vacancy

trait TagExtractor {
  def extractTags(ds: Dataset[Vacancy], sourceCol: String): DataFrame
}