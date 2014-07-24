import com.mle.sbtutils.SbtUtils
import sbt._
import sbt.Keys._

/**
 * The build.
 */
object PlayBuild extends Build {

  lazy val utilPlay = SbtUtils.testableProject("util-play").settings(projectSettings: _*)

  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3"

  lazy val projectSettings = SbtUtils.publishSettings ++ Seq(
    version := "1.4.3",
    scalaVersion := "2.11.2",
//    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % "2.3.2",
      "com.typesafe.play" %% "play-ws" % "2.3.2",
      "com.github.malliina" %% "util" % "1.3.2",
      "com.ning" % "async-http-client" % "1.8.9",
      httpGroup % "httpclient" % httpVersion,
      httpGroup % "httpmime" % httpVersion),
    fork in Test := true,
    exportJars := false,
    resolvers += "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
  )
}