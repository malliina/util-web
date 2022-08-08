scalaVersion := "2.12.16"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.2.15",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0",
  "org.scala-js" % "sbt-scalajs" % "1.10.1",
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
) map addSbtPlugin
