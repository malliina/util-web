import com.mle.sbtutils.{SbtProjects, SbtUtils}
import sbt.Keys._
import sbt._

/**
 * The build.
 */
object PlayBuild extends Build {
  lazy val utilPlay = SbtProjects.mavenPublishProject("util-play").settings(projectSettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3.5"
  val playGroup = "com.typesafe.play"
  val playVersion = "2.3.4"
  val mleGroup = "com.github.malliina"

  lazy val projectSettings = Seq(
    version := "1.6.10",
    scalaVersion := "2.11.4",
    //    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-ws" % playVersion,
      mleGroup %% "util" % "1.5.0",
      mleGroup %% "logback-rx" % "0.1.2",
      "com.ning" % "async-http-client" % "1.8.13",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true,
    exportJars := false,
    resolvers += "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
  )
}