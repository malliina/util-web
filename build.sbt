import com.malliina.sbtutils.SbtUtils
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import play.core.PlayVersion
import play.sbt.PlayImport

val httpGroup = "org.apache.httpcomponents"
val httpVersion = "4.5.3"
val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"

lazy val root = project.in(file("."))
  .aggregate(utilPlay, htmlJvm, htmlJs)
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val utilPlay = Project("util-play", file("util-play"))
  .settings(utilPlaySettings: _*)
  .dependsOn(htmlJvm)
  .disablePlugins(BintrayPlugin)

lazy val html = crossProject.in(file("util-html"))
  .settings(htmlSettings: _*)
  .jvmSettings(htmlJvmSettings: _*)
  .jsSettings(htmlJsSettings: _*)

lazy val htmlJvm = html.jvm.disablePlugins(BintrayPlugin)
lazy val htmlJs = html.js.disablePlugins(BintrayPlugin)

def utilPlaySettings = commonSettings ++ libSettings ++ Seq(
  crossScalaVersions := Seq("2.11.11", scalaVersion.value),
  releaseCrossBuild := true,
  scalacOptions := Seq("-unchecked", "-deprecation"),

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
)

def htmlJvmSettings = commonSettings ++ Seq(

)

def htmlJsSettings = commonSettings ++ Seq(

)

def commonSettings = SbtUtils.mavenSettings ++ Seq(
  scalaVersion := "2.12.4",
  organization := "com.malliina",
  gitUserName := "malliina",
  developerName := "Michael Skogberg",
  publishTo := Option(Opts.resolver.sonatypeStaging),
  publishArtifact in Test := true
)

def htmlSettings = Seq(
  name := "util-html",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "scalatags" % "0.6.7",
    "com.typesafe.play" %%% "play-json" % "2.6.8",
    "com.malliina" %%% "primitives" % "1.3.2"
  )
)

def libSettings = Seq(
  resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
  libraryDependencies ++= defaultDeps
)

def defaultDeps = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % Test,
  PlayImport.specs2 % Test
)
