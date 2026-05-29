package org.example.habrcareer.config

import org.example.core.config.loader.ArgsLoader
import org.example.core.config.loader.module.WithCommonArgsConfig

class HabrArgsLoader(args: Array[String])
  extends ArgsLoader(args)
    with WithCommonArgsConfig
{
  verify()
}
