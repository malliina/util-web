import com.mle.sbtutils.SbtProjects
import com.mle.sbtutils.SbtUtils.{developerName, gitUserName}
import sbt.Keys._
import sbt._

/**
 * The build.
 */
object PlayBuild extends Build {
  lazy val utilPlay = SbtProjects.testableProject("util-play", file("util-play"))
    .enablePlugins(bintray.BintrayPlugin).settings(projectSettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.5"
  val playGroup = "com.typesafe.play"
  val playVersion = "2.4.2"
  val mleGroup = "com.github.malliina"

  lazy val projectSettings = Seq(
    version := "2.2.0",
    scalaVersion := "2.11.7",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    organization := s"com.github.${gitUserName.value}",
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-ws" % playVersion,
      playGroup %% "play-netty-server" % playVersion,
      mleGroup %% "util" % "1.9.0",
      mleGroup %% "logback-rx" % "0.3.0",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpcore" % "4.4.1",
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true,
    resolvers ++= Seq(sbt.Resolver.jcenterRepo),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  )
}
