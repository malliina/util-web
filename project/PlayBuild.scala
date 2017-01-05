import bintray.Plugin.bintraySettings
import com.malliina.sbtplay.PlayProject
import play.core.PlayVersion
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.PlayScala
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._

object PlayBuild {

  lazy val root = Project("root", file("."))
    .aggregate(utilPlay, playBase)
    .settings(rootSettings: _*)

  lazy val playBase = Project("play-base", file("play-base"))
    .enablePlugins(PlayScala)
    .settings(baseSettings: _*)
    .dependsOn(utilPlay)

  lazy val utilPlay = PlayProject.library("util-play", file("util-play"))
    .settings(utilPlaySettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.5.2"
  val playGroup = "com.typesafe.play"
  val playVersion = PlayVersion.current
  val malliinaGroup = "com.malliina"

  lazy val baseSettings = bintraySettings ++ Seq(
    version := "3.3.3",
    scalaVersion := "2.11.8",
    RoutesKeys.routesGenerator := InjectedRoutesGenerator,
    organization := s"com.malliina",
    resolvers += Resolver.jcenterRepo,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  )

  lazy val rootSettings = baseSettings ++ Seq(
    publishArtifact := false,
    publishTo := None
  )

  lazy val utilPlaySettings = baseSettings ++ Seq(
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-ws" % playVersion,
      playGroup %% "play-netty-server" % playVersion,
      malliinaGroup %% "util" % "2.4.1",
      malliinaGroup %% "logback-rx" % "1.0.2",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpcore" % "4.4.4",
      httpGroup % "httpmime" % httpVersion
    ),
    fork in Test := true
  )
}
