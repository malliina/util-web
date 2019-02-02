scalaVersion := "2.12.8"
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
classpathTypes += "maven-plugin"

def ivyRepo(name: String, urlString: String) =
  Resolver.url(name, url(urlString))(Resolver.ivyStylePatterns)

Seq(
  "com.malliina" %% "sbt-utils-maven" % "0.11.0",
  "com.malliina" %% "sbt-play" % "1.5.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0",
  "org.scala-js" % "sbt-scalajs" % "0.6.26"
) map addSbtPlugin
