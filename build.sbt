import com.malliina.sbtutils.MavenCentralKeys
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

val malliinaGroup = "com.malliina"

val versions = new {
  val primitives = "3.7.7"
  val munit = "1.1.0"
  val scalatags = "0.13.1"
}

inThisBuild(
  Seq(
    organization := "com.malliina",
    scalaVersion := "3.3.1",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    Test / publishArtifact := true,
    libraryDependencies += "org.scalameta" %% "munit" % versions.munit % Test
  )
)

val commonsCodec = "commons-codec" % "commons-codec" % "1.18.0"

val webAuth = Project("web-auth", file("web-auth"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    libraryDependencies ++= Seq(
      malliinaGroup %% "okclient-io" % versions.primitives,
      "com.nimbusds" % "nimbus-jose-jwt" % "10.0.1",
      commonsCodec
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val html = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("util-html"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    name := "util-html",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "case-insensitive" % "1.4.2",
      "com.lihaoyi" %%% "scalatags" % versions.scalatags,
      malliinaGroup %%% "primitives" % versions.primitives
    ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val htmlJvm = html.jvm
val htmlJs = html.js

val database = project
  .in(file("database"))
  .enablePlugins(MavenCentralPlugin)
  .settings(
    libraryDependencies ++=
      Seq("config", "okclient-io").map { m =>
        malliinaGroup %%% m % versions.primitives
      } ++ Seq("core", "hikari").map { m =>
        "org.tpolecat" %% s"doobie-$m" % "1.0.0-RC6"
      } ++ Seq(
        "org.flywaydb" % "flyway-mysql" % "11.3.0"
      ),
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val http4s = project
  .in(file("http4s"))
  .dependsOn(htmlJvm)
  .enablePlugins(MavenCentralPlugin)
  .settings(
    name := "util-http4s",
    libraryDependencies ++=
      Seq("ember-server", "circe", "dsl").map { m =>
        "org.http4s" %% s"http4s-$m" % "0.23.30"
      },
    releaseProcess := MavenCentralKeys.tagReleaseProcess.value
  )

val webAuthRoot = project
  .in(file("."))
  .aggregate(webAuth, htmlJvm, htmlJs, database, http4s)
  .settings(
    releaseProcess := (webAuth / releaseProcess).value,
    organization := malliinaGroup,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
