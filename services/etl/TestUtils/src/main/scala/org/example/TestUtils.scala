package org.example

import scala.io.Source
import scala.util.{Failure, Success, Using}

object TestUtils {

  def readFile(path: String): String = {
    val res = Using(Source.fromResource(path)) { source => source.mkString }

    res match {
      case Success(content) => content
      case Failure(e) => throw e
    }
  }

}
