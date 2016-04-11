import com.malliina.sbtplay.PlayProject
import com.malliina.sbtutils.SbtProjects
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import play.core.PlayVersion
import sbt.Keys._
import sbt._

/**
 * The build.
 */
object PlayBuild extends Build {

  lazy val root = Project("root", file("."))
    .aggregate(utilPlay, playBase)
    .settings(rootSettings: _*)

  lazy val playBase = PlayProject("play-base", file("play-base"))
    .settings(baseSettings: _*)
    .enablePlugins(bintray.BintrayPlugin)
    .dependsOn(utilPlay)

  lazy val utilPlay = SbtProjects.testableProject("util-play", file("util-play"))
    .enablePlugins(bintray.BintrayPlugin)
    .settings(utilPlaySettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.5.2"
  val playGroup = "com.typesafe.play"
  val playVersion = PlayVersion.current
  val malliinaGroup = "com.malliina"

  lazy val baseSettings = Seq(
    version := "2.7.1",
    scalaVersion := "2.11.7",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    organization := s"com.${gitUserName.value}",
    resolvers ++= Seq(
      sbt.Resolver.jcenterRepo,
      "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
    ),
    licenses +=("MIT", url("http://opensource.org/licenses/MIT"))
  )

  lazy val rootSettings = baseSettings ++ Seq(
    publishArtifact := false,
    publishTo := None
  )

  lazy val utilPlaySettings = baseSettings ++ Seq(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-ws" % playVersion,
      playGroup %% "play-netty-server" % playVersion,
      malliinaGroup %% "util" % "2.1.0",
      malliinaGroup %% "logback-rx" % "1.0.0",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpcore" % "4.4.4",
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true
  )
}
