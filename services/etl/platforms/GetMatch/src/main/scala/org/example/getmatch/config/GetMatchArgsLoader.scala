package org.example.getmatch.config

import org.example.core.config.loader.ArgsLoader
import org.example.core.config.loader.module.WithCommonArgsConfig


class GetMatchArgsLoader(args: Array[String]) extends ArgsLoader(args)
  with WithCommonArgsConfig {
  verify()
}