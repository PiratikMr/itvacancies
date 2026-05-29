package org.example.core.adapter.database.impl.postgres

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType
import org.example.SparkEnv
import org.example.core.config.model.structures.DBConf
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper


class PostgresAdapterTest extends AnyFlatSpec with ForAllTestContainer with SparkEnv {

  override val container: PostgreSQLContainer = PostgreSQLContainer(
    dockerImageNameOverride = "postgres:17-alpine"
  ).configure { c =>
    c.withInitScript("init.sql")
  }

  private lazy val dbConf: DBConf = DBConf(
    container.username,
    container.password,
    container.host,
    container.mappedPort(5432).toString,
    container.databaseName,
    1,
    1000
  )

  import spark.implicits._

  private lazy val adapter = new PostgresAdapter(dbConf)

  "PostgresAdapter" should "create table and save data" in {
    val tableName = "test_table"

    val inputData = Seq(
      (1, "NewUser")
    ).toDF("id", "name")

    adapter.save(inputData, tableName, Seq("id"))
    val result = adapter.loadTable(spark, tableName)

    assertSchemaEquals(inputData.schema, result.schema)
    assertDataEquals(inputData, result)
  }

  "PostgresAdapter" should "perform UPSERT correctly" in {
    val tableName = "test_upsert_table"

    val initialData = Seq(
      (1, "Alice", "Designer"),
      (5, "Bob", "Analytic")
    ).toDF("id", "name", "role")

    val updates = Seq(
      (1, "Alice", "Software Developer"),
      (5, "Garmin", "Analytic"),
      (6, "Carol", "HR")
    ).toDF("id", "name", "role")

    val expectedData = updates

    adapter.save(initialData, tableName, Seq("id"))
    adapter.save(updates, tableName, Seq("id"), Some(Seq("name", "role")))

    val actualResultFromDB = adapter.loadTable(spark, tableName)

    assertSchemaEquals(expectedData.schema, actualResultFromDB.schema)
    assertDataEquals(expectedData, actualResultFromDB)
  }

  "PostgresAdapter" should "load data using custom query" in {
    val tableName = "test_query_table"
    val data = Seq((1, "Alice"), (2, "Bob")).toDF("id", "name")
    adapter.save(data, tableName, Seq("id"))

    val result = adapter.loadQuery(spark, s"SELECT id FROM $tableName WHERE name = 'Alice'")
    result.count() shouldBe 1
    result.collect().head.getInt(0) shouldBe 1
  }

  "PostgresAdapter" should "save data and return returning columns" in {
    val tableName = "test_return_table"
    val data = Seq((1, "Alice")).toDF("id", "name")

    val result = adapter.saveWithReturn(spark, data, tableName, Seq("id", "name"), Seq("id"))

    result.count() shouldBe 1
    assertDataEquals(data, result)
  }

  private def assertSchemaEquals(expected: StructType, actual: StructType): Unit = {
    actual.length shouldBe expected.length

    actual.zip(expected).foreach { case (fieldActual, fieldExpected) =>
      fieldActual.name shouldBe fieldExpected.name
      fieldActual.dataType shouldBe fieldExpected.dataType
    }
  }

  private def assertDataEquals(expected: DataFrame, actual: DataFrame): Unit = {
    val actualRows = actual.collect().sortBy(_.getInt(0)).toList
    val expectedRows = expected.collect().sortBy(_.getInt(0)).toList

    actualRows shouldBe expectedRows
  }
}
