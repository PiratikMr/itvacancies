package org.example.core.adapter.web

import org.example.core.adapter.web.impl.sttp.model.{WebError, WebResponse}

trait WebAdapter extends AutoCloseable with Serializable {

  def execute(url: String): Either[WebError, WebResponse]

  def readBody(url: String): Either[WebError, String]

  def readBodyOrNone(url: String): Option[String]

  def readBodyOrThrow(url: String): String

}