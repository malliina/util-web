import sbt.Keys._
import sbt._

object BuildBuild extends Build {

  override lazy val settings = super.settings ++ sbtPlugins ++ Seq(
    Keys.scalaVersion := "2.10.6",
    resolvers ++= Seq(
      Resolver.url("bintray-sbt-plugin-releases",
        url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
      Resolver.url("malliina bintray sbt",
        url("https://dl.bintray.com/malliina/sbt-plugins/"))(Resolver.ivyStylePatterns)
    )
  )

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-play" % "0.8.2",
    "me.lessis" % "bintray-sbt" % "0.2.1"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}
