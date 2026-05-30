package org.example.TestData

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.adapter.storage.StorageAdapter
import org.example.core.adapter.web.WebAdapter
import org.scalatestplus.mockito.MockitoSugar

trait Mocks extends MockitoSugar {

  val mockSpark: SparkSession = mock[SparkSession]

  val mockDataFrame: DataFrame = mock[DataFrame]
  val mockDatasetString: Dataset[String] = mock[Dataset[String]]

  val mockDBService: DataBaseAdapter = mock[DataBaseAdapter]
  val mockStorageService: StorageAdapter = mock[StorageAdapter]
  val mockWebService: WebAdapter = mock[WebAdapter]
}
