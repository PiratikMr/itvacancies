package org.example.core.config.loader

import com.typesafe.config.{Config, ConfigFactory}

import java.io.File

abstract class FileLoader[T](val confPath: String) {
  protected lazy val rootConfig: Config = ConfigFactory.parseFile(new File(confPath)).resolve()
}
