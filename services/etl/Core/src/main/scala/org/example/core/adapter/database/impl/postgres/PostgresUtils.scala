package org.example.core.adapter.database.impl.postgres

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.example.core.config.model.structures.DBConf

import java.sql.{Connection, DriverManager}
import java.util.UUID
import scala.util.Try
import scala.util.control.NonFatal

object PostgresUtils extends LazyLogging {

  def loadTable(spark: SparkSession, conf: DBConf, targetTable: String): DataFrame = {
    logger.info(s"Загрузка всей таблицы $targetTable из БД ${conf.base}")
    readJdbc(spark, conf, "dbtable", targetTable)
  }

  def loadQuery(spark: SparkSession, conf: DBConf, query: String): DataFrame = {
    logger.info(s"Загрузка данных из БД ${conf.base} с помощью пользовательского запроса")
    logger.debug(s"Выполнение SQL запроса:\n$query")
    readJdbc(spark, conf, "query", query)
  }

  def save(conf: DBConf,
           df: DataFrame,
           targetTable: String,
           conflicts: Seq[String],
           updates: Option[Seq[String]]): Unit = {

    logger.info(s"Старт операции SAVE (upsert) для таблицы $targetTable. Ключи конфликта: ${conflicts.mkString(", ")}")

    withStaging(conf, df.dropDuplicates(conflicts)) { stagingTable =>
      executeInTransaction(conf) { conn =>
        val sql = buildUpsertQuery(targetTable, stagingTable, df.columns, conflicts, updates)
        executeSql(conn, sql)
      }
    }

    logger.info(s"Операция SAVE (upsert) для таблицы $targetTable успешно завершена")
  }

  def saveWithReturn(
                      spark: SparkSession,
                      conf: DBConf,
                      df: DataFrame,
                      targetTable: String,
                      returns: Seq[String],
                      conflicts: Seq[String],
                      updates: Option[Seq[String]]
                    ): DataFrame = {

    logger.info(s"Старт операции SAVE_WITH_RETURN (upsert) для таблицы $targetTable. Ключи конфликта: ${conflicts.mkString(", ")}")

    withStaging(conf, df.dropDuplicates(conflicts)) { stagingTable =>
      executeInTransaction(conf) { conn =>
        val sql = buildUpsertQuery(targetTable, stagingTable, df.columns, conflicts, updates)
        executeSql(conn, sql)
      }

      logger.debug(s"Чтение возвращаемых колонок: ${returns.mkString(", ")}")

      val joinCondition = conflicts.map(c => s"t.$c = s.$c").mkString(" AND ")
      val returnCols = returns.map(c => s"t.$c").mkString(", ")

      val result = loadQuery(spark, conf,
        s"""
           |SELECT $returnCols
           |FROM $targetTable as t
           |JOIN $stagingTable as s ON $joinCondition
           |""".stripMargin
      )

      val checkpointedResult = result.localCheckpoint()
      logger.info(s"Операция SAVE_WITH_RETURN успешно завершена для $targetTable")
      checkpointedResult
    }
  }

  def update(conf: DBConf,
             df: DataFrame,
             targetTable: String,
             joinColumns: Seq[String],
             updateColumns: Seq[String]): Unit = {

    logger.info(s"Старт операции UPDATE для таблицы $targetTable. Join ключи: ${joinColumns.mkString(", ")}")

    withStaging(conf, df.dropDuplicates(joinColumns)) { stagingTable =>
      executeInTransaction(conf) { conn =>
        val setClause = updateColumns.map(c => s"$c = s.$c").mkString(", ")
        val whereClause = joinColumns.map(c => s"t.$c = s.$c").mkString(" AND ")

        val sql =
          s"""
             |UPDATE $targetTable as t
             |SET $setClause
             |FROM $stagingTable as s
             |WHERE $whereClause
             |""".stripMargin

        executeSql(conn, sql)
      }
    }

    logger.info(s"Операция UPDATE для таблицы $targetTable успешно завершена")
  }


  private def withStaging[T](conf: DBConf, df: DataFrame)(block: String => T): T = {
    val stagingTable = s"staging_${UUID.randomUUID().toString.replace("-", "")}"

    logger.debug(s"Создание и запись во временную staging-таблицу: $stagingTable")

    try {
      writeDefault(conf, df, stagingTable)
      block(stagingTable)

    } finally {
      logger.debug(s"Удаление временной staging-таблицы: $stagingTable")
      Try(dropTable(conf, stagingTable)).recover {
        case NonFatal(e) => logger.error(s"Не удалось удалить staging-таблицу $stagingTable", e)
      }
    }
  }

  private def executeInTransaction[T](conf: DBConf)(block: Connection => T): T = {
    logger.debug("Открытие соединения и начало транзакции")

    val conn = getConnection(conf)
    try {
      conn.setAutoCommit(false)
      val result = block(conn)
      conn.commit()

      logger.debug("Транзакция успешно закоммичена")

      result

    } catch {
      case NonFatal(e) =>

        logger.error("Ошибка при выполнении транзакции, выполняется rollback()", e)

        Try(conn.rollback())
        throw e

    } finally {
      logger.debug("Закрытие соединения с БД")

      Try(conn.close()).recover {
        case NonFatal(e) => logger.error("Ошибка при закрытии соединения", e)
      }
    }
  }

  private def dropTable(conf: DBConf, tableName: String): Unit = {
    val conn = getConnection(conf)
    try {
      executeSql(conn, s"DROP TABLE IF EXISTS $tableName")
    } finally {
      conn.close()
    }
  }

  private def executeSql(conn: Connection, sql: String): Unit = {
    val stmt = conn.createStatement()
    try {
      logger.debug(s"Выполнение SQL команды:\n$sql")

      stmt.execute(sql)

    } finally {
      stmt.close()
    }
  }


  private def buildUpsertQuery(
                                target: String,
                                source: String,
                                columns: Seq[String],
                                conflicts: Seq[String],
                                updates: Option[Seq[String]]
                              ): String = {

    val colStr = columns.mkString(", ")

    val conflictColStr = conflicts.mkString(", ")

    val conflictAction = updates match {
      case Some(updCols) if updCols.nonEmpty =>
        val setClause = updCols.map(c => s"$c = EXCLUDED.$c").mkString(", ")
        s"DO UPDATE SET $setClause"
      case _ =>
        "DO NOTHING"
    }

    s"""
       |INSERT INTO $target ($colStr)
       |SELECT $colStr FROM $source
       |ON CONFLICT ($conflictColStr)
       |$conflictAction
       |""".stripMargin
  }


  private def baseOptions(conf: DBConf): Map[String, String] = Map(
    "driver" -> "org.postgresql.Driver",
    "url" -> conf.url,
    "user" -> conf.name,
    "password" -> conf.pass
  )

  private val JDBC = "jdbc"

  private def readJdbc(spark: SparkSession, conf: DBConf, key: String, value: String): DataFrame = {
    spark.read
      .format(JDBC)
      .options(baseOptions(conf))
      .option(key, value)
      .load()
  }

  private def writeDefault(conf: DBConf, df: DataFrame, targetTable: String): Unit = {
    df.write
      .format(JDBC)
      .mode(SaveMode.Append)
      .options(baseOptions(conf))
      .option("dbtable", targetTable)
      .option("batchsize", conf.batchSize.toString)
      .option("numPartitions", conf.maxPartitions.toString)
      .save()
  }

  private def getConnection(conf: DBConf): Connection =
    try {
      DriverManager.getConnection(conf.url, conf.name, conf.pass)

    } catch {
      case NonFatal(e) =>
        logger.error(s"Не удалось установить подключение с БД по URL: ${conf.url}", e)
        throw e
    }
}