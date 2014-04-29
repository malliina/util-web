import com.mle.sbtutils.SbtUtils
import sbt._
import sbt.Keys._

/**
 * A scala build file template.
 */
object PlayBuild extends Build {

  import Dependencies._

  lazy val utilPlay = Project("util-play", file(".")).settings(projectSettings: _*)

  lazy val projectSettings = SbtUtils.publishSettings ++ Seq(
    version := "1.3.0",
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(scalaTest, play, utilDep, httpClient, httpMime),
    scalaVersion := "2.10.4",
    fork in Test := true,
    exportJars := false
  )
}

object Dependencies {
  val utilDep = "com.github.malliina" %% "util" % "1.3.0"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.0" % "test"
  val play = "com.typesafe.play" %% "play" % "2.2.2"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3"
  val httpClient = httpGroup % "httpclient" % httpVersion
  val httpMime = httpGroup % "httpmime" % httpVersion
}