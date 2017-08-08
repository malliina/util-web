import com.malliina.sbtplay.PlayProject
import com.malliina.sbtutils.SbtUtils
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import play.core.PlayVersion

val httpGroup = "org.apache.httpcomponents"
val httpVersion = "4.5.3"
val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"

lazy val utilPlay = PlayProject.library("util-play").settings(SbtUtils.mavenSettings: _*)

scalaVersion := "2.12.3"
crossScalaVersions := Seq("2.11.11", scalaVersion.value)
releaseCrossBuild := true
scalacOptions := Seq("-unchecked", "-deprecation")
gitUserName := "malliina"
developerName := "Michael Skogberg"
organization := "com.malliina"

publishArtifact in Test := true

libraryDependencies ++= Seq(
  playGroup %% "play" % playVersion,
  playGroup %% "play-netty-server" % playVersion,
  playGroup %% "play-ahc-ws-standalone" % "1.0.4",
  playGroup %% "play-ws-standalone-json" % "1.0.4",
  malliinaGroup %% "util" % "2.8.0",
  malliinaGroup %% "logback-rx" % "1.2.0",
  httpGroup % "httpclient" % httpVersion,
  httpGroup % "httpcore" % "4.4.6",
  httpGroup % "httpmime" % httpVersion,
  "org.scala-stm" %% "scala-stm" % "0.8"
)
