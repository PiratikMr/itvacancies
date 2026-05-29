package org.example.geekjob.config

import org.example.core.config.loader.ArgsLoader
import org.example.core.config.loader.module.WithCommonArgsConfig

class GeekJobArgsLoader(args: Array[String])
  extends ArgsLoader(args)
    with WithCommonArgsConfig
{
  verify()
}
