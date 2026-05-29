package org.example.core.config.model.structures

case class SparkConf(
               name: String,
               master: String,
               driverMemory: String,
               driverCores: String,
               executorMemory: String,
               executorCores: String
               )