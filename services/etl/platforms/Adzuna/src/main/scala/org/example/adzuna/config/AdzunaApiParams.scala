package org.example.adzuna.config

case class AdzunaApiParams(
                            locationTag: String,
                            maxDaysOld: Int,
                            vacsPerPage: Int,
                            appId: String,
                            appKey: String,
                            categoryTag: String
                          )
