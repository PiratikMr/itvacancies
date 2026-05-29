package org.example.core.config.model.structures

case class StructuresConfig(
                             dbConf: DBConf,
                             fsConf: FSConf,
                             sparkConf: SparkConf,
                             netConf: NetworkConf,
                             fuzzyMatcherConf: FuzzyMatcherConf
                           )
