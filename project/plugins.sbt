scalaVersion := "2.12.20"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.6.46",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "org.scala-js" % "sbt-scalajs" % "1.18.2",
  "org.scalameta" % "sbt-scalafmt" % "2.5.4"
) map addSbtPlugin
