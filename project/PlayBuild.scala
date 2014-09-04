import com.mle.sbtutils.SbtUtils
import sbt.Keys._
import sbt._

/**
 * The build.
 */
object PlayBuild extends Build {

  lazy val utilPlay = SbtUtils.testableProject("util-play").settings(projectSettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3.5"
  val playGroup = "com.typesafe.play"
  val playVersion = "2.3.4"

  lazy val projectSettings = SbtUtils.publishSettings ++ Seq(
    version := "1.5.5",
    scalaVersion := "2.11.2",
    //    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-ws" % playVersion,
      "com.github.malliina" %% "util" % "1.3.2",
      "com.ning" % "async-http-client" % "1.8.13",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true,
    exportJars := false,
    resolvers += "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
  )
}