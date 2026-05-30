package org.example.geekjob.implement

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.example.core.adapter.database.DataBaseAdapter
import org.example.core.config.model.structures.FuzzyMatcherConf
import org.example.core.etl.model.{NormalizedVacancy, Vacancy, VacancyColumns}
import org.example.core.etl.Transformer
import org.example.core.normalization.factory.NormalizerFactory
import org.example.core.normalization.model.NormalizersEnum._
import org.example.core.normalization.api.NormalizationTask.Exact
import org.example.core.normalization.service.NormalizationOrchestrator
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.CollectionHasAsScala

class GeekJobTransformer(currDate: String,
                         dbAdapter: DataBaseAdapter,
                         fuzzyConf: FuzzyMatcherConf) extends Transformer {

  override def toRows(spark: SparkSession, rawDS: Dataset[String]): DataFrame = {

    val currYear: String = currDate.substring(0, 4)

    val rowsRDD = rawDS.rdd.mapPartitions { part =>
      part.map {
        row => GeekJobTransformer.transformHelper(row, currYear)
      }
    }

    spark.createDataFrame(rowsRDD, GeekJobTransformer.schema)
  }

  override def transform(spark: SparkSession, df: DataFrame): Dataset[Vacancy] = {
    import spark.implicits._

    df
      .select(
        col("id").as(VacancyColumns.EXTERNAL_ID),
        lit("GeekJob").as(VacancyColumns.PLATFORM),
        col("employer").as(VacancyColumns.EMPLOYER),
        col("currency").as(VacancyColumns.CURRENCY),
        col("experience").as(VacancyColumns.EXPERIENCE),

        col("salary_from").cast(DoubleType).as(VacancyColumns.SALARY_FROM),
        col("salary_to").cast(DoubleType).as(VacancyColumns.SALARY_TO),
        lit(null).cast(DoubleType).as(VacancyColumns.LATITUDE),
        lit(null).cast(DoubleType).as(VacancyColumns.LONGITUDE),

        col("publish_date").as(VacancyColumns.PUBLISHED_AT),
        col("title").as(VacancyColumns.TITLE),
        lit(null).cast(StringType).as(VacancyColumns.DESCRIPTION),
        concat(lit("https://geekjob.ru/vacancy/"), col("id")).as(VacancyColumns.URL),

        col("job_format").as(VacancyColumns.EMPLOYMENTS),
        col("job_format").as(VacancyColumns.SCHEDULES),

        functions.transform(
          col("locations"),
          loc => struct(
            loc.as(VacancyColumns.LOCATION),
            loc.as(VacancyColumns.COUNTRY)
          )
        ).as(VacancyColumns.LOCATIONS),

        col("fields").as(VacancyColumns.FIELDS),
        col("specs").as(VacancyColumns.SKILLS),
        col("level").as(VacancyColumns.GRADES),
        typedLit(Seq.empty[org.example.core.etl.model.Language]).as(VacancyColumns.LANGUAGES)
      ).as[Vacancy]
  }

  override def normalize(spark: SparkSession, transformedData: Dataset[Vacancy]): Dataset[NormalizedVacancy] = {

    new NormalizationOrchestrator(spark, dbAdapter, fuzzyConf)
      .normalize(Seq(
        CURRENCY,
        EMPLOYER,
        EXPERIENCE,
        FIELDS,
        PLATFORM,
        SKILLS,
        GRADES,
        Exact(EMPLOYMENTS),
        Exact(SCHEDULES)
      ), transformedData)
  }

}

private object GeekJobTransformer {
  private val schema: StructType = StructType(Seq(
    StructField("id", StringType),
    StructField("title", StringType),
    StructField("employer", StringType),
    StructField("experience", StringType),
    StructField("locations", ArrayType(StringType)),
    StructField("publish_date", TimestampType),
    StructField("job_format", ArrayType(StringType)),
    StructField("salary_from", LongType),
    StructField("salary_to", LongType),
    StructField("currency", StringType),
    StructField("specs", ArrayType(StringType)),
    StructField("fields", ArrayType(StringType)),
    StructField("level", ArrayType(StringType))
  ))

  private def transformHelper(row: String, currYear: String): Row = {
    val Doc: Document = Jsoup.parse(row.substring(24))
    val Header: Elements = Doc.select("header")
    val Company: Elements = Header.select("h5.company-name")
    val JobInfo: Elements = Header.select("div.jobinfo")
    val Salary: String = JobInfo.select("span.salary").text()
    val SalaryArray: Array[Long] =
      """(\d{1,3}(?:\s\d{3})*)""".r.findAllIn(Salary).map(_.replaceAll("\\s", "").toLong)
        .toArray


    val id: String = row.substring(0, 24)
    val title: String = Header.select("h1").text()

    val company = if (Company.html().contains("Частный рекрутер")) null
    else Company.select("a").first() match {
      case a: Element => a.text()
      case _ => null
    }

    val locations = Header.select("div.location").text().split(", ").map(_.trim)

    val date: Timestamp = dateTransform(Header.select("div.time").text(), currYear)

    val (exp, jobFormat) = transformJobFormat(JobInfo.select("span.jobformat").html())

    val salary_from = if (SalaryArray.nonEmpty) SalaryArray.head else null
    val salary_to = if (SalaryArray.length > 1) SalaryArray(1) else null

    val currency: String = currencyMap.collectFirst {
      case (symbol, code) if Salary.contains(symbol) => code
    }.orNull

    val (specs, fields, level) = tagsTransform(Doc.select("div.tags-list"))


    Row(id, title, company, exp, locations, date, jobFormat, salary_from, salary_to, currency, specs, fields, level)
  }

  private lazy val currencyMap: Map[String, String] = Map(
    "$" -> "USD",
    "€" -> "EUR",
    "K" -> "PGK",
    "₽" -> "RUB"
  )

  private lazy val monthsMap: Map[String, String] = Map(
    "января" -> "01", "февраля" -> "02", "марта" -> "03", "апреля" -> "04",
    "мая" -> "05", "июня" -> "06", "июля" -> "07", "августа" -> "08",
    "сентября" -> "09", "октября" -> "10", "ноября" -> "11", "декабря" -> "12"
  )

  private def dateTransform(raw: String, currYear: String): Timestamp = {
    val times: Array[String] = raw.split(" ")

    val day: String = if (times(0).length < 2) s"0${times(0)}" else times(0)
    val year: String = if (times.length > 2) times(2) else currYear

    val dateStr: String = s"$year.${monthsMap(times(1))}.$day"

    try {
      Timestamp.valueOf(java.time.LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy.MM.dd")).atStartOfDay())
    } catch {
      case _: Exception => null
    }
  }

  private def transformJobFormat(raw: String): (String, Array[String]) = {
    val temp = raw.split("<br>").map(_.trim)

    val expOption: Option[String] = temp.find(_.toLowerCase.contains("опыт"))

    val exp = expOption match {
      case Some(t) =>
        val numbers = """\d+""".r.findAllIn(t).map(_.toInt).toList
        if (numbers.size == 2) {
          val f = if (numbers.head == 1) "года " else ""
          val s = if (numbers(1) > 1) "лет" else "года"
          s"От ${numbers.head} ${f}до ${numbers(1)} $s"
        } else if (numbers.size == 1) {
          val f = if (numbers.head == 1) "года" else "лет"
          val op = if (t.contains("менее")) "Менее" else "Более"
          s"$op ${numbers.head} $f"
        } else "Любой"
      case None => null
    }

    val jobFormat = temp.filter(o => !o.toLowerCase.contains("опыт")).flatMap(_.split("•")).map(_.trim)
      .filter(_.nonEmpty)

    (exp, jobFormat)
  }

  private def tagsTransform(divTag: Elements): (Array[String], Array[String], Array[String]) = {

    val specs_t: Array[String] = divTag.select("b:contains(Специализация) + br ~ a.chip:not(b:contains(Отрасль) ~ a.chip)").eachText().asScala.toArray
    val specs: Array[String] = specs_t.filter(str => "[а-яА-ЯёЁ]".r.findFirstIn(str).isEmpty)

    (
      specs,
      divTag.select("b:contains(Отрасль и сфера применения) + br ~ a.chip:not(b:contains(Уровень) ~ a.chip)").eachText().asScala.toArray,
      divTag.select("b:contains(Уровень должности) + br ~ a.chip").eachText().asScala.toArray
    )
  }
}