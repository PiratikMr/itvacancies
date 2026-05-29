package org.example.core.etl

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.example.SparkEnv
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.adapter.storage.StorageAdapter
import org.example.core.adapter.web.WebAdapter
import org.example.core.etl.model.{NormalizedVacancy, Vacancy}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar

class ETLUServiceTest extends AnyFlatSpec with MockitoSugar with SparkEnv {

  import spark.implicits._

  "ETLUService" should "execute EXTRACT pipeline correctly" in {
    val dbAdapter = mock[DataBaseAdapter]
    val storageAdapter = mock[StorageAdapter]
    val webAdapter = mock[WebAdapter]
    val extractor = mock[Extractor]
    val transformer = mock[Transformer]

    val service = new ETLUService(spark, dbAdapter, storageAdapter, webAdapter)

    val fakeRawDs = Seq("data").toDS()
    when(extractor.extract(any[SparkSession], any[WebAdapter])).thenReturn(fakeRawDs)

    service.run(
      etlPart = "extract",
      extractor = extractor,
      transformer = transformer,
      platformName = "testPlatform",
      folderName = "TestFolder"
    )

    verify(extractor, times(1)).extract(spark, webAdapter)
    verify(storageAdapter, times(1)).writeText(fakeRawDs, "TestFolder")
    verifyNoInteractions(transformer)
  }

  "ETLUService" should "execute TRANSFORM_LOAD pipeline correctly" in {
    val dbAdapter = mock[DataBaseAdapter]
    val storageAdapter = mock[StorageAdapter]
    val webAdapter = mock[WebAdapter]
    val extractor = mock[Extractor]
    val transformer = mock[Transformer]
    val loader = mock[Loader]

    val service = new ETLUService(spark, dbAdapter, storageAdapter, webAdapter)

    val fakeRawDs = Seq.empty[String].toDS()
    val fakeRawDf = Seq.empty[String].toDF("value")
    val fakeTransformedDs = Seq.empty[Vacancy].toDS()
    val fakeNormalizedDs = Seq.empty[NormalizedVacancy].toDS()

    when(storageAdapter.readText(any[SparkSession], any[String])).thenReturn(fakeRawDs)
    when(transformer.toRows(any[SparkSession], any[Dataset[String]])).thenReturn(fakeRawDf)
    when(transformer.transform(any[SparkSession], any[DataFrame])).thenReturn(fakeTransformedDs)
    when(transformer.normalize(any[SparkSession], any[Dataset[Vacancy]])).thenReturn(fakeNormalizedDs)

    service.run(
      etlPart = "transform-load",
      extractor = extractor,
      transformer = transformer,
      loader = Some(loader),
      platformName = "testPlatform",
      folderName = "TestFolder"
    )

    verify(storageAdapter, times(1)).readText(spark, "TestFolder")
    verify(transformer, times(1)).toRows(any[SparkSession], any[Dataset[String]])
    verify(transformer, times(1)).transform(any[SparkSession], any[DataFrame])
    verify(transformer, times(1)).normalize(any[SparkSession], any[Dataset[Vacancy]])
    verify(loader, times(1)).load(any[SparkSession], any[Dataset[NormalizedVacancy]])
    verifyNoInteractions(extractor)
  }

  "ETLUService" should "throw exception when updating without updateLimit" in {
    val dbAdapter = mock[DataBaseAdapter]
    val storageAdapter = mock[StorageAdapter]
    val webAdapter = mock[WebAdapter]
    val extractor = mock[Extractor]
    val transformer = mock[Transformer]

    val service = new ETLUService(spark, dbAdapter, storageAdapter, webAdapter)

    service.run(
      etlPart = "update",
      extractor = extractor,
      transformer = transformer,
      platformName = "testPlatform",
      folderName = "TestFolder"
    )

    verifyNoInteractions(extractor)
  }

  "ETLUService" should "throw exception for unknown ETL part" in {
    val dbAdapter = mock[DataBaseAdapter]
    val storageAdapter = mock[StorageAdapter]
    val webAdapter = mock[WebAdapter]
    val extractor = mock[Extractor]
    val transformer = mock[Transformer]

    val service = new ETLUService(spark, dbAdapter, storageAdapter, webAdapter)

    intercept[IllegalArgumentException] {
      service.run(
        etlPart = "unknown",
        extractor = extractor,
        transformer = transformer,
        platformName = "testPlatform"
      )
    }
  }

}
