package org.example.core.etl.model

import java.sql.Timestamp

case class Location(location: Option[String], country: Option[String])

case class Language(language: String, level: Option[String])

case class NormalizedLanguage(languageId: Long, levelId: Option[Long])


case class Vacancy(
                    externalId: String,
                    title: String,
                    description: Option[String],
                    url: String,
                    platform: String,

                    latitude: Option[Double],
                    longitude: Option[Double],

                    salaryFrom: Option[Double],
                    salaryTo: Option[Double],

                    employer: Option[String],
                    currency: Option[String],
                    experience: Option[String],

                    employments: Seq[String],
                    schedules: Seq[String],
                    fields: Seq[String],
                    grades: Seq[String],
                    skills: Seq[String],

                    languages: Seq[Language],
                    locations: Seq[Location],

                    publishedAt: Timestamp
                  )


case class NormalizedVacancy(
                              externalId: String,
                              title: String,
                              url: String,

                              latitude: Option[Double],
                              longitude: Option[Double],

                              salaryFrom: Option[Double],
                              salaryTo: Option[Double],

                              platformId: Long,
                              employerId: Option[Long],
                              currencyId: Option[Long],
                              experienceId: Option[Long],

                              employmentIds: Seq[Long],
                              scheduleIds: Seq[Long],
                              fieldIds: Seq[Long],
                              gradeIds: Seq[Long],
                              skillIds: Seq[Long],

                              languages: Seq[NormalizedLanguage],
                              locations: Seq[Long],

                              publishedAt: Timestamp
                            )

object VacancyColumns {
  val EXTERNAL_ID = "externalId"
  val TITLE = "title"
  val DESCRIPTION = "description"
  val URL = "url"
  val PLATFORM = "platform"
  val PUBLISHED_AT = "publishedAt"

  val LATITUDE = "latitude"
  val LONGITUDE = "longitude"

  val SALARY_FROM = "salaryFrom"
  val SALARY_TO = "salaryTo"

  val EMPLOYER = "employer"
  val CURRENCY = "currency"
  val EXPERIENCE = "experience"

  val EMPLOYMENTS = "employments"
  val SCHEDULES = "schedules"
  val FIELDS = "fields"
  val GRADES = "grades"
  val SKILLS = "skills"
  val LANGUAGES = "languages"
  val LOCATIONS = "locations"

  val PLATFORM_ID = "platformId"
  val EMPLOYER_ID = "employerId"
  val CURRENCY_ID = "currencyId"
  val EXPERIENCE_ID = "experienceId"

  val EMPLOYMENT_IDS = "employmentIds"
  val SCHEDULE_IDS = "scheduleIds"
  val FIELD_IDS = "fieldIds"
  val GRADE_IDS = "gradeIds"
  val SKILL_IDS = "skillIds"

  val LOCATION = "location"
  val COUNTRY = "country"
  val LANGUAGE = "language"
  val LEVEL = "level"
  val LANGUAGE_ID = "languageId"
  val LEVEL_ID = "levelId"
}