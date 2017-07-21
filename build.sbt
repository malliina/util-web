import com.malliina.sbtplay.PlayProject
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import play.core.PlayVersion

val httpGroup = "org.apache.httpcomponents"
val httpVersion = "4.5.3"
val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"

lazy val utilPlay = PlayProject.library("util-play")

scalaVersion := "2.12.2"
crossScalaVersions := Seq("2.11.11", scalaVersion.value)
releaseCrossBuild := true
gitUserName := "malliina"
developerName := "Michael Skogberg"
organization := "com.malliina"

publishArtifact in Test := true

libraryDependencies ++= Seq(
  playGroup %% "play" % playVersion,
  playGroup %% "play-netty-server" % playVersion,
  playGroup %% "play-ahc-ws-standalone" % "1.0.1",
  playGroup %% "play-ws-standalone-json" % "1.0.1",
  malliinaGroup %% "util" % "2.6.0",
  malliinaGroup %% "logback-rx" % "1.2.0",
  httpGroup % "httpclient" % httpVersion,
  httpGroup % "httpcore" % "4.4.6",
  httpGroup % "httpmime" % httpVersion,
  "org.scala-stm" %% "scala-stm" % "0.8"
)
