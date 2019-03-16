import play.core.PlayVersion
import play.sbt.PlayImport
import sbtcrossproject.CrossPlugin.autoImport.{crossProject => portableProject, CrossType => PortableType}

val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"
val primitiveVersion = "1.9.0"

val baseSettings = Seq(
  scalaVersion := "2.12.8",
  organization := "com.malliina"
)

val commonResolvers = Seq(
  resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
)

val commonSettings = baseSettings ++ commonResolvers ++ Seq(
  gitUserName := "malliina",
  developerName := "Michael Skogberg",
  publishTo := Option(Opts.resolver.sonatypeStaging),
  publishArtifact in Test := true
)

val commonsCodec = "commons-codec" % "commons-codec" % "1.12"

val playCommon = Project("play-common", file("play-common"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      "com.malliina" %%% "primitives" % primitiveVersion
    )
  )

val playSocial = Project("play-social", file("play-social"))
  .enablePlugins(MavenCentralPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      playGroup %% "play" % playVersion,
      malliinaGroup %% "okclient" % primitiveVersion,
      "com.nimbusds" % "nimbus-jose-jwt" % "7.0.1",
      commonsCodec,
      "org.scalatest" %% "scalatest" % "3.0.6" % Test
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
      "com.lihaoyi" %%% "scalatags" % "0.6.7",
      "com.typesafe.play" %%% "play-json" % "2.7.1",
      "com.malliina" %%% "primitives" % primitiveVersion,
      "org.scalatest" %%% "scalatest" % "3.0.6" % Test
    )
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
      malliinaGroup %% "logback-streams" % "1.5.0",
      commonsCodec,
      "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test,
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
