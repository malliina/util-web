import com.mle.sbtutils.{SbtProjects, SbtUtils}
import sbt.Keys._
import sbt._
import bintray.Plugin.bintraySettings

/**
 * The build.
 */
object PlayBuild extends Build {
  lazy val utilPlay = SbtProjects.mavenPublishProject("util-play").settings(projectSettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3.5"
  val playGroup = "com.typesafe.play"
  val playVersion = "2.3.8"
  val mleGroup = "com.github.malliina"

  lazy val projectSettings = bintraySettings ++ Seq(
    version := "1.9.1",
    scalaVersion := "2.11.6",
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-ws" % playVersion,
      mleGroup %% "util" % "1.8.1",
      mleGroup %% "logback-rx" % "0.2.0",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true,
    exportJars := false,
    resolvers ++= Seq(
      "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
      sbt.Resolver.jcenterRepo,
      "Bintray malliina" at "http://dl.bintray.com/malliina/maven"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  )
}
