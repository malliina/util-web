import sbt.Keys._
import sbt._

object BuildBuild {

  val settings = sbtPlugins ++ Seq(
    Keys.scalaVersion := "2.10.6",
    resolvers ++= Seq(
      Resolver.url("bintray-sbt-plugin-releases",
        url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
      Resolver.url("malliina bintray sbt",
        url("https://dl.bintray.com/malliina/sbt-plugins/"))(Resolver.ivyStylePatterns)
    )
  )

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-utils" % "0.6.1",
    "com.malliina" %% "sbt-play" % "0.9.2"
  ) map addSbtPlugin
}
