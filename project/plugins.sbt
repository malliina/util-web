scalaVersion := "2.12.10"

Seq(
  "com.malliina" % "sbt-utils-maven" % "0.16.1",
  "com.typesafe.play" % "sbt-plugin" % "2.8.1",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0",
  "org.scala-js" % "sbt-scalajs" % "0.6.32",
  "ch.epfl.scala" % "sbt-bloop" % "1.3.4",
  "org.scalameta" % "sbt-scalafmt" % "2.3.0"
) map addSbtPlugin
