package org.example.core.config.loader

import org.rogach.scallop.ScallopConf

abstract class ArgsLoader(args: Array[String]) extends ScallopConf(args)