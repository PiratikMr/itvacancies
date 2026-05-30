package org.example.finder.config

import org.example.core.config.loader.ArgsLoader
import org.example.core.config.loader.module.WithCommonArgsConfig

class FinderArgsLoader(args: Array[String])
  extends ArgsLoader(args)
    with WithCommonArgsConfig {
  verify()
}
