import com.malliina.sbtutils.SbtUtils
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import play.core.PlayVersion
import play.sbt.PlayImport
import sbtcrossproject.CrossPlugin.autoImport.{crossProject => portableProject, CrossType => PortableType}

val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"

lazy val utilPlayRoot = project.in(file("."))
  .aggregate(utilPlay, playSocial, htmlJvm, htmlJs)
  .settings(
    organization := malliinaGroup,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

// TODO remove dep on playSocial
lazy val utilPlay = Project("util-play", file("util-play"))
  .settings(utilPlaySettings: _*)
  .dependsOn(playSocial, htmlJvm)

lazy val playSocial = Project("play-social", file("play-social"))
  .settings(playSocialSettings: _*)

lazy val html = portableProject.crossType(PortableType.Full).in(file("util-html"))
  .settings(htmlSettings: _*)
  .jvmSettings(htmlJvmSettings: _*)
  .jsSettings(htmlJsSettings: _*)

lazy val htmlJvm = html.jvm
lazy val htmlJs = html.js

def utilPlaySettings = commonSettings ++ libSettings ++ Seq(
  releaseCrossBuild := true,
  scalacOptions := Seq("-unchecked", "-deprecation"),

  libraryDependencies ++= Seq(
    playGroup %% "play" % playVersion,
    playGroup %% "play-server" % playVersion,
    malliinaGroup %% "util" % "2.10.2",
    malliinaGroup %% "util-rmi" % "2.10.2",
    malliinaGroup %% "logback-rx" % "1.2.0",
    "org.scala-stm" %% "scala-stm" % "0.8"
  )
)

def playSocialSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    playGroup %% "play" % playVersion,
    malliinaGroup %% "okclient" % "1.5.2",
    "com.nimbusds" % "nimbus-jose-jwt" % "5.12",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
)

def htmlJvmSettings = commonSettings ++ Seq(

)

def htmlJsSettings = commonSettings ++ Seq(

)

def commonSettings = SbtUtils.mavenSettings ++ commonResolvers ++ Seq(
  scalaVersion := "2.12.6",
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
    "com.typesafe.play" %%% "play-json" % "2.6.9",
    "com.malliina" %%% "primitives" % "1.5.2",
    "org.scalatest" %%% "scalatest" % "3.0.5"
  )
)

def libSettings = commonResolvers ++ Seq(
  libraryDependencies ++= defaultDeps
)

def commonResolvers = Seq(
  resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
)

def defaultDeps = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  PlayImport.specs2 % Test
)
