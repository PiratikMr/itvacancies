package org.example.core.config.model.structures

case class FSConf(
                   url: String,
                   rootPath: String,
                   platform: String,
                   dateFolder: String
                 ) {

  def getPath(folderName: String, withDate: Boolean = true): String = {

    val base = s"$url/$rootPath/$platform/$folderName"

    if (withDate) s"$base/$dateFolder"
    else base
  }
}