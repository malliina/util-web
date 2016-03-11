import sbt.Keys._
import sbt._

object BuildBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    Keys.scalaVersion := "2.10.6",
    resolvers ++= Seq(
      Resolver.url("bintray-sbt-plugin-releases",
        url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
      Resolver.url("malliina bintray sbt",
        url("https://dl.bintray.com/malliina/sbt-plugins/"))(Resolver.ivyStylePatterns)
    )
  ) ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-play" % "0.7.0",
    "me.lessis" % "bintray-sbt" % "0.3.0"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}
