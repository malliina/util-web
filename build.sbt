import com.malliina.sbtutils.MavenCentralKeys
import sbtcrossproject.CrossPlugin.autoImport.{CrossType => PortableType, crossProject => portableProject}
val malliinaGroup = "com.malliina"
val primitiveVersion = "3.4.4"
val munitVersion = "0.7.29"
val scalatagsVersion = "0.12.0"

inThisBuild(
  Seq(
    organization := "com.malliina",
    scalaVersion := "3.3.0",
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
    libraryDependencies ++= Seq(
      malliinaGroup %% "okclient-io" % primitiveVersion,
      "com.nimbusds" % "nimbus-jose-jwt" % "9.31",
      commonsCodec
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val html = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("util-html"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    name := "util-html",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % scalatagsVersion,
      malliinaGroup %%% "primitives" % primitiveVersion
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val htmlJvm = html.jvm
val htmlJs = html.js

val database = project
  .in(file("database"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    libraryDependencies ++= Seq("config", "okclient-io").map { m =>
      malliinaGroup %%% m % primitiveVersion
    } ++ Seq("core", "hikari").map { m =>
      "org.tpolecat" %% s"doobie-$m" % "1.0.0-RC4"
    } ++ Seq(
      "org.flywaydb" % "flyway-core" % "7.15.0"
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val webAuthRoot = project
  .in(file("."))
  .aggregate(webAuth, htmlJvm, htmlJs, database)
  .settings(
    releaseProcess := (webAuth / releaseProcess).value,
    organization := malliinaGroup,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
