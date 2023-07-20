scalaVersion := "2.12.17"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.6.19",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "org.scala-js" % "sbt-scalajs" % "1.13.2",
  "org.scalameta" % "sbt-scalafmt" % "2.5.0"
) map addSbtPlugin
