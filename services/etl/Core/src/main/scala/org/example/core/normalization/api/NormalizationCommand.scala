package org.example.core.normalization.api

import org.apache.spark.sql.{DataFrame, Dataset}
import org.example.core.etl.model.Vacancy

trait NormalizationCommand {
  def execute(ds: Dataset[Vacancy]): DataFrame
}
