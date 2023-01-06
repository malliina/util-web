scalaVersion := "2.12.17"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.4.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0",
  "org.scala-js" % "sbt-scalajs" % "1.12.0",
  "org.scalameta" % "sbt-scalafmt" % "2.5.0"
) map addSbtPlugin
