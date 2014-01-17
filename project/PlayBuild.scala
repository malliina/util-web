import sbt._
import sbt.Keys._

/**
 * A scala build file template.
 */
object PlayBuild extends Build {

  import Dependencies._

  lazy val utilPlay = Project("util-play", file("."), settings = projectSettings)
    .settings(libraryDependencies ++= Seq(scalaTest, play, utilDep, httpClient, httpMime))

  val releaseVersion = "1.1.0"
  val snapshotVersion = "1.1.1-SNAPSHOT"

  lazy val projectSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.10.3",
    fork in Test := true,
    organization := "com.github.malliina",
    name := "util-play",
    version := releaseVersion,
    exportJars := false,
    publishTo := {
      val repo =
        if (version.value endsWith "SNAPSHOT") {
          "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
        } else {
          "Sonatype releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        }
      Some(repo)
    },
    licenses += ("BSD-style" -> url("http://www.opensource.org/licenses/BSD-3-Clause")),
    scmInfo := Some(ScmInfo(url("https://github.com/malliina/util-play"), "git@github.com:malliina/util-play.git")),
    credentials += Credentials(Path.userHome / ".ivy2" / "sonatype.txt"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false),
    pomExtra := extraPom
  )

  def extraPom = (
    <url>https://github.com/malliina/util-play</url>
      <developers>
        <developer>
          <id>malliina</id>
          <name>Michael Skogberg</name>
          <url>http://mskogberg.info</url>
        </developer>
      </developers>)
}

object Dependencies {
  val utilDep = "com.github.malliina" %% "util" % "1.0.0"
  val scalaTest = "org.scalatest" %% "scalatest" % "1.9.2" % "test"
  val play = "com.typesafe.play" %% "play" % "2.2.1"
  val httpGroup = "org.apache.httpcomponents"
  val httpVersion = "4.3"
  val httpClient = httpGroup % "httpclient" % httpVersion
  val httpMime = httpGroup % "httpmime" % httpVersion
}