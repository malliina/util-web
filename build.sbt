import play.core.PlayVersion
import play.sbt.PlayImport
import sbtcrossproject.CrossPlugin.autoImport.{CrossType => PortableType, crossProject => portableProject}

val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"
val primitiveVersion = "1.11.0"
val scalaTestVersion = "3.0.8"

val baseSettings = Seq(
  scalaVersion := "2.13.0",
  crossScalaVersions := scalaVersion.value :: "2.12.8" :: Nil,
  organization := "com.malliina",
  releaseProcess := tagReleaseProcess.value
)

val commonSettings = baseSettings ++ Seq(
  gitUserName := "malliina",
  developerName := "Michael Skogberg",
  publishArtifact in Test := true
)

val commonsCodec = "commons-codec" % "commons-codec" % "1.12"

val playCommon = project.in(file("play-common"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      "com.malliina" %%% "primitives" % primitiveVersion
    )
  )

val playSocial = project.in(file("play-social"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      malliinaGroup %% "okclient" % primitiveVersion,
      "com.nimbusds" % "nimbus-jose-jwt" % "7.3",
      commonsCodec,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )
  .dependsOn(playCommon)

val html = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("util-html"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    name := "util-html",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.7.0",
      "com.typesafe.play" %%% "play-json" % "2.7.4",
      "com.malliina" %%% "primitives" % primitiveVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
    )
  )

val htmlJvm = html.jvm
val htmlJs = html.js

val utilPlay = project.in(file("util-play"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    releaseCrossBuild := true,
    scalacOptions := Seq("-unchecked", "-deprecation"),
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-server" % playVersion,
      malliinaGroup %% "okclient" % primitiveVersion,
      malliinaGroup %% "logback-streams" % "1.6.0",
      commonsCodec,
      "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
      PlayImport.specs2 % Test
    )
  )
  .dependsOn(htmlJvm, playCommon)

val utilPlayRoot = project.in(file("."))
  .aggregate(utilPlay, playSocial, htmlJvm, htmlJs, playCommon)
  .settings(baseSettings)
  .settings(
    organization := malliinaGroup,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )
