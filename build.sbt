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
val primitiveVersion = "1.12.3"
val scalaTestVersion = "3.1.0"

val baseSettings = Seq(
  scalaVersion := "2.13.1",
  crossScalaVersions := scalaVersion.value :: "2.12.10" :: Nil,
  organization := "com.malliina"
)

val commonSettings = baseSettings ++ Seq(
  gitUserName := "malliina",
  developerName := "Michael Skogberg",
  publishArtifact in Test := true
)

val commonsCodec = "commons-codec" % "commons-codec" % "1.13"

val playCommon = Project("play-common", file("play-common"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      "com.malliina" %%% "primitives" % primitiveVersion
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val playSocial = Project("play-social", file("play-social"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      malliinaGroup %% "okclient" % primitiveVersion,
      "com.nimbusds" % "nimbus-jose-jwt" % "8.3",
      commonsCodec,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
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
      "com.typesafe.play" %%% "play-json" % "2.8.1",
      "com.malliina" %%% "primitives" % primitiveVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val htmlJvm = html.jvm
val htmlJs = html.js

val utilPlay = Project("util-play", file("util-play"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    releaseCrossBuild := true,
    scalacOptions := Seq("-unchecked", "-deprecation"),
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      playGroup %% "play-server" % playVersion,
      malliinaGroup %% "okclient" % primitiveVersion,
      malliinaGroup %% "logback-streams" % "1.7.0",
      commonsCodec,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
      PlayImport.specs2 % Test
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )
  .dependsOn(htmlJvm, playCommon)

val utilPlayRoot = project
  .in(file("."))
  .aggregate(utilPlay, playSocial, htmlJvm, htmlJs, playCommon)
  .settings(baseSettings)
  .settings(
    releaseProcess := (releaseProcess in utilPlay).value,
    organization := malliinaGroup,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )
