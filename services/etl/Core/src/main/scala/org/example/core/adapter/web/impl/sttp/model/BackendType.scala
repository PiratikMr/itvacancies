package org.example.core.adapter.web.impl.sttp.model

sealed trait BackendType

object BackendType {

  case object Default extends BackendType

  case object UnsafeSSL extends BackendType

}