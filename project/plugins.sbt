scalaVersion := "2.12.15"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.2.13",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0",
  "org.scala-js" % "sbt-scalajs" % "1.9.0",
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
) map addSbtPlugin
