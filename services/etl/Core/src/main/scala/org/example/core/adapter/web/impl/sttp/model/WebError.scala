package org.example.core.adapter.web.impl.sttp.model

sealed trait WebError extends Exception


case class HttpError(statusCode: Int, body: String) extends WebError {
  override def getMessage: String = s"HTTP $statusCode: $body"
}

case class ConnectionError(cause: Throwable) extends WebError {
  override def getMessage: String = s"Connection failed: ${cause.getMessage}"
}

case class ParsingError(msg: String) extends WebError