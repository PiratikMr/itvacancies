package org.example.core.etl.model

import scala.util.{Failure, Success, Try}

sealed abstract class ETLPart(val cliName: String)

object ETLParts {

  case object Extract extends ETLPart("extract")
  case object TransformLoad extends ETLPart("transform-load")
  case object Update extends ETLPart("update")

  val values: Seq[ETLPart] = Seq(Extract, TransformLoad, Update)

  def parse(str: String): Try[ETLPart] = {
    val normalized = str.trim.toLowerCase

    values.find(_.cliName  == normalized) match {
      case Some(part) => Success(part)
      case None =>
        val available = values.map(_.cliName).mkString(", ")
        Failure(new IllegalArgumentException(s"Неизвестный ETL модуль: '$str'. Доступные варианты: $available"))
    }
  }
}