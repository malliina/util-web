scalaVersion := "2.12.14"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.2.4",
  "com.typesafe.play" % "sbt-plugin" % "2.8.8",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0",
  "org.scala-js" % "sbt-scalajs" % "1.7.0",
  "org.scalameta" % "sbt-scalafmt" % "2.4.3"
) map addSbtPlugin
