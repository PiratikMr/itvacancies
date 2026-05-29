package org.example.core.config.model.structures

case class DBConf(
                 name: String,
                 pass: String,
                 host: String,
                 port: String,
                 base: String,

                 maxPartitions: Integer,
                 batchSize: Integer
                 ) {
  def url: String = s"jdbc:postgresql://$host:$port/$base"
}