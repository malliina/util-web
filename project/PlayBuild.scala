import com.mle.sbtplay.PlayProject
import com.mle.sbtutils.SbtProjects
import com.mle.sbtutils.SbtUtils.{developerName, gitUserName}
import sbt.Keys._
import sbt._

/**
 * The build.
 */
object PlayBuild extends Build {

  lazy val root = Project("root", file("."))
    .aggregate(utilPlay, playBase)
    .settings(rootSettings: _*)

  lazy val utilPlay = SbtProjects.testableProject("util-play", file("util-play"))
    .enablePlugins(bintray.BintrayPlugin)
    .settings(utilPlaySettings: _*)

  lazy val playBase = PlayProject("play-base", file("play-base"))
    .settings(baseSettings: _*)
    .enablePlugins(bintray.BintrayPlugin)
    .dependsOn(utilPlay)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.5"
  val playGroup = "com.typesafe.play"
  val playVersion = "2.4.2"
  val mleGroup = "com.github.malliina"

  lazy val baseSettings = Seq(
    version := "2.3.0",
    scalaVersion := "2.11.7",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    organization := s"com.github.${gitUserName.value}",
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
      mleGroup %% "util" % "2.0.0",
      mleGroup %% "logback-rx" % "0.4.0",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpcore" % "4.4.2",
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true
  )
}
