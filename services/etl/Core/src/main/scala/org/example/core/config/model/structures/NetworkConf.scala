package org.example.core.config.model.structures

case class NetworkConf(
                    headers: Array[(String, String)],
                    requestsPS: Int,
                    timeout: Int
                  )
