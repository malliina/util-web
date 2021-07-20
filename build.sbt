import com.malliina.sbtutils.MavenCentralKeys
import play.core.PlayVersion
import play.sbt.PlayImport
import sbtcrossproject.CrossPlugin.autoImport.{
  CrossType => PortableType,
  crossProject => portableProject
}
import com.malliina.sbtutils.SbtUtils

val playGroup = "com.typesafe.play"
val playVersion = PlayVersion.current
val malliinaGroup = "com.malliina"
val primitiveVersion = "2.0.2"
val munitVersion = "0.7.27"
val scalatagsVersion = "0.9.4"
val playJsonVersion = "2.9.2"

inThisBuild(
  Seq(
    organization := "com.malliina",
    scalaVersion := "3.0.0",
    crossScalaVersions := scalaVersion.value :: "2.13.6" :: Nil,
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    Test / publishArtifact := true,
    libraryDependencies += "org.scalameta" %% "munit" % munitVersion % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val commonsCodec = "commons-codec" % "commons-codec" % "1.15"

val webAuth = Project("web-auth", file("web-auth"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    libraryDependencies ++= SbtUtils.loggingDeps ++ Seq(
      malliinaGroup %% "okclient-io" % primitiveVersion,
      "com.nimbusds" % "nimbus-jose-jwt" % "9.10.1",
      commonsCodec
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

//val playCommon = Project("play-common", file("play-common"))
//  .enablePlugins(MavenCentralPlugin)
//  .dependsOn(webAuth)
//  .settings(
//    libraryDependencies ++= Seq(
//      playGroup %% "play" % playVersion,
//      malliinaGroup %%% "primitives" % primitiveVersion
//    ),
//    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
//  )

//val playSocial = Project("play-social", file("play-social"))
//  .enablePlugins(MavenCentralPlugin)
//  .dependsOn(webAuth)
//  .settings(
//    libraryDependencies ++= Seq(
//      playGroup %% "play" % playVersion,
//      commonsCodec
//    ),
//    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
//  )
//  .dependsOn(playCommon)

val html = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("util-html"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    name := "util-html",
    libraryDependencies ++= Seq(
      ("com.lihaoyi" %%% "scalatags" % scalatagsVersion).cross(CrossVersion.for3Use2_13),
      malliinaGroup %%% "primitives" % primitiveVersion
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val htmlJvm = html.jvm
val htmlJs = html.js

//val utilPlay = Project("util-play", file("util-play"))
//  .enablePlugins(MavenCentralPlugin)
//  .settings(
//    releaseCrossBuild := true,
//    scalacOptions := Seq("-unchecked", "-deprecation"),
//    libraryDependencies ++= Seq(
//      playGroup %% "play" % playVersion,
//      playGroup %% "play-server" % playVersion,
//      commonsCodec,
//      PlayImport.specs2 % Test
//    ),
//    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
//  )
//  .dependsOn(htmlJvm, playCommon)

val webAuthRoot = project
  .in(file("."))
  //  .aggregate(utilPlay, playSocial, webAuth, htmlJvm, htmlJs, playCommon)
  .aggregate(webAuth, htmlJvm, htmlJs)
  .settings(
    releaseProcess := (webAuth / releaseProcess).value,
    organization := malliinaGroup,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
