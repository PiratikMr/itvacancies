import sbt.Keys.libraryDependencies
import sbtassembly.AssemblyPlugin.autoImport.assembly

import scala.collection.Seq

lazy val sparkVersion = "4.0.0"

ThisBuild / version := "1"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
)

ThisBuild / Test / fork := true
ThisBuild / Test / javaOptions ++= Seq(
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "-Djava.security.manager=allow",
  "-Dnet.bytebuddy.experimental=true"
)

lazy val assemblySettings = Seq(
  assembly / assemblyMergeStrategy := {
    case PathList("module-info.class") => MergeStrategy.discard
    case PathList("META-INF", "versions", xs@_, "module-info.class") => MergeStrategy.discard
    case x => (assembly / assemblyMergeStrategy).value(x)
  }
)

lazy val TestUtils = (project in file("TestUtils"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.18",
      "org.mockito" % "mockito-core" % "5.11.0",
      "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0",
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.44.1",
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.44.1"
    )
  )

lazy val Core = (project in file("Core"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.3",
      "org.rogach" %% "scallop" % "5.2.0",
      "com.github.rholder" % "snowball-stemmer" % "1.3.0.581.1",
      "com.softwaremill.sttp.client4" %% "core" % "4.0.10",
      "org.postgresql" % "postgresql" % "42.7.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
    )
  ).dependsOn(TestUtils % Test)


val platformFolder = "platforms/"


lazy val Currency = (project in file("Currency"))
  .settings(
    assemblySettings,
    assembly / mainClass := Some("org.example.currency.CurrencyMain"),
    assembly / assemblyJarName := "Currency-etl.jar"
  )
  .dependsOn(Core)
  .dependsOn(TestUtils % Test)


lazy val Finder = (project in file(s"${platformFolder}Finder"))
  .settings(
    assemblySettings,
    assembly / mainClass := Some("org.example.finder.FinderMain"),
    assembly / assemblyJarName := "Finder-etl.jar"
  )
  .dependsOn(Core)
  .dependsOn(TestUtils % Test)

lazy val GeekJob = (project in file(s"${platformFolder}GeekJob"))
  .settings(
    assemblySettings,
    libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.2.1",
    assembly / mainClass := Some("org.example.geekjob.GeekJobMain"),
    assembly / assemblyJarName := "GeekJob-etl.jar"
  )
  .dependsOn(Core)

lazy val GetMatch = (project in file(s"${platformFolder}GetMatch"))
  .settings(
    assemblySettings,
    assembly / mainClass := Some("org.example.getmatch.GetMatchMain"),
    assembly / assemblyJarName := "GetMatch-etl.jar"
  )
  .dependsOn(Core)


lazy val HeadHunter = (project in file(s"${platformFolder}HeadHunter"))
  .settings(
    assemblySettings,
    assembly / mainClass := Some("org.example.headhunter.HeadHunterMain"),
    assembly / assemblyJarName := "HeadHunter-etl.jar"
  )
  .dependsOn(Core)
  .dependsOn(TestUtils % Test)

lazy val HabrCareer = (project in file(s"${platformFolder}HabrCareer"))
  .settings(
    assemblySettings,
    assembly / mainClass := Some("org.example.habrcareer.HabrCareerMain"),
    assembly / assemblyJarName := "HabrCareer-etl.jar"
  )
  .dependsOn(Core)

lazy val Adzuna = (project in file(s"${platformFolder}Adzuna"))
  .settings(
    assemblySettings,
    assembly / mainClass := Some("org.example.adzuna.AdzunaMain"),
    assembly / assemblyJarName := "Adzuna-etl.jar"
  )
  .dependsOn(Core)

addCommandAlias("buildAllPlatforms",
  "; clean" +
    "; all" +
    " Currency/assembly" +
    " Finder/assembly" +
    " GeekJob/assembly" +
    " GetMatch/assembly" +
    " HeadHunter/assembly" +
    " HabrCareer/assembly" +
    " Adzuna/assembly"
)