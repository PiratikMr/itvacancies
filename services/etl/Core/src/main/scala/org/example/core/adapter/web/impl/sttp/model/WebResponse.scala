package org.example.core.adapter.web.impl.sttp.model

case class WebResponse(body: String,
                       statusCode: Integer,
                       headers: Map[String, String])
