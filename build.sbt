import com.malliina.sbtplay.PlayProject
import com.malliina.sbtutils.SbtUtils
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import play.core.PlayVersion

val httpGroup = "org.apache.httpcomponents"
val httpVersion = "4.5.3"
val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"

lazy val utilPlay = PlayProject.library("util-play").settings(SbtUtils.mavenSettings: _*).disablePlugins(BintrayPlugin)

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.11", scalaVersion.value)
releaseCrossBuild := true
scalacOptions := Seq("-unchecked", "-deprecation")
gitUserName := "malliina"
developerName := "Michael Skogberg"
organization := "com.malliina"

publishTo := Option(Opts.resolver.sonatypeStaging)
publishArtifact in Test := true

libraryDependencies ++= Seq(
  playGroup %% "play" % playVersion,
  playGroup %% "play-server" % playVersion,
  malliinaGroup %% "util" % "2.8.2",
  malliinaGroup %% "util-rmi" % "2.8.2",
  malliinaGroup %% "logback-rx" % "1.2.0",
  httpGroup % "httpclient" % httpVersion,
  httpGroup % "httpcore" % "4.4.8",
  httpGroup % "httpmime" % httpVersion,
  "org.scala-stm" %% "scala-stm" % "0.8"
)
