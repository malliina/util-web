import com.mle.sbtutils.SbtUtils
import sbt._
import sbt.Keys._

/**
 * The build.
 */
object PlayBuild extends Build {

  lazy val utilPlay = SbtUtils.testableProject("util-play").settings(projectSettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3"

  lazy val projectSettings = SbtUtils.publishSettings ++ Seq(
    version := "1.4.0",
    scalaVersion := "2.10.4",
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % "2.2.3",
      "com.github.malliina" %% "util" % "1.3.1",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true,
    exportJars := false
  )
}