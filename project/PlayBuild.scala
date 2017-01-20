import com.malliina.sbtplay.PlayProject
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName, mavenSettings}
import play.core.PlayVersion
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._

object PlayBuild {
  lazy val utilPlay = PlayProject.library("util-play")
    .settings(utilPlaySettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.5.2"
  val playGroup = "com.typesafe.play"
  val playVersion = PlayVersion.current
  val malliinaGroup = "com.malliina"

  lazy val baseSettings = mavenSettings ++ Seq(
    version := "3.5.1",
    scalaVersion := "2.11.8",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    organization := s"com.malliina",
    RoutesKeys.routesGenerator := InjectedRoutesGenerator,
    resolvers += Resolver.jcenterRepo
  )

  lazy val utilPlaySettings = baseSettings ++ Seq(
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-ws" % playVersion,
      playGroup %% "play-netty-server" % playVersion,
      malliinaGroup %% "util" % "2.5.0",
      malliinaGroup %% "logback-rx" % "1.1.0",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpcore" % "4.4.4",
      httpGroup % "httpmime" % httpVersion
    )
  )
}
