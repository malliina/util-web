import sbt.Keys._
import sbt._

object BuildBuild {
  lazy val settings = sbtPlugins ++ Seq(
    scalaVersion := "2.10.6",
    resolvers ++= Seq(
      // temporary hack
      // http://stackoverflow.com/a/42211230
      "JBoss" at "https://repository.jboss.org/",
      ivyRepo("bintray-sbt-plugin-releases",
        "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
      ivyRepo("malliina bintray sbt",
        "https://dl.bintray.com/malliina/sbt-plugins/"),
      Resolver.bintrayRepo("malliina", "maven")
    )
  )

  def ivyRepo(name: String, urlString: String) =
    Resolver.url(name, url(urlString))(Resolver.ivyStylePatterns)

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-utils" % "0.6.1",
    "com.malliina" %% "sbt-play" % "0.9.7"
  ) map addSbtPlugin
}
