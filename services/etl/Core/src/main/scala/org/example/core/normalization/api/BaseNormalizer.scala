package org.example.core.normalization.api

import org.apache.spark.sql.{DataFrame, Dataset}
import org.example.core.etl.model.Vacancy

trait BaseNormalizer {
  def process(ds: Dataset[Vacancy], withCreate: Boolean): DataFrame
}
