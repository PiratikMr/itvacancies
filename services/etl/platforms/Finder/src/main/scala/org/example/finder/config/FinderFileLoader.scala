package org.example.finder.config

import org.example.core.config.loader.FileLoader
import org.example.core.config.loader.module.{WithCommonFileConfig, WithStandardStructures}

class FinderFileLoader(confPath: String, override val saveFolder: String)
  extends FileLoader(confPath)
    with WithStandardStructures
    with WithCommonFileConfig
    with WithFinderFileConfig {
}
