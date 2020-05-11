import com.malliina.sbtutils.MavenCentralKeys
import play.core.PlayVersion
import play.sbt.PlayImport
import sbtcrossproject.CrossPlugin.autoImport.{
  CrossType => PortableType,
  crossProject => portableProject
}

val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"
val primitiveVersion = "1.16.0"
val munitVersion = "0.7.6"
val scalatagsVersion = "0.9.1"

inThisBuild(
  Seq(
    organization := "com.malliina",
    scalaVersion := "2.13.2",
    crossScalaVersions := scalaVersion.value :: Nil,
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    publishArtifact in Test := true,
    libraryDependencies += "org.scalameta" %% "munit" % munitVersion % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val commonsCodec = "commons-codec" % "commons-codec" % "1.14"

val playCommon = Project("play-common", file("play-common"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      "com.malliina" %%% "primitives" % primitiveVersion
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val playSocial = Project("play-social", file("play-social"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      malliinaGroup %% "okclient" % primitiveVersion,
      "com.nimbusds" % "nimbus-jose-jwt" % "8.16",
      commonsCodec
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )
  .dependsOn(playCommon)

val html = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("util-html"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    name := "util-html",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % scalatagsVersion,
      "com.typesafe.play" %% "play-json" % "2.8.1",
      "com.malliina" %%% "primitives" % primitiveVersion
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val htmlJvm = html.jvm
val htmlJs = html.js

val utilPlay = Project("util-play", file("util-play"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    releaseCrossBuild := true,
    scalacOptions := Seq("-unchecked", "-deprecation"),
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-server" % playVersion,
      malliinaGroup %% "okclient" % primitiveVersion,
//      malliinaGroup %% "logback-streams" % "1.7.2",
      commonsCodec,
      PlayImport.specs2 % Test
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )
  .dependsOn(htmlJvm, playCommon)

val utilPlayRoot = project
  .in(file("."))
  .aggregate(utilPlay, playSocial, htmlJvm, htmlJs, playCommon)
  .settings(
    releaseProcess := (releaseProcess in utilPlay).value,
    organization := malliinaGroup,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
