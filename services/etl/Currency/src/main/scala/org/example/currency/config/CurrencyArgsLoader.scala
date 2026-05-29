package org.example.currency.config

import org.example.core.config.loader.ArgsLoader
import org.example.core.config.loader.module.WithCommonArgsConfig


class CurrencyArgsLoader(args: Array[String])
  extends ArgsLoader(args)
    with WithCommonArgsConfig {
  verify()
}
