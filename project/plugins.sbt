scalaVersion := "2.12.10"

Seq(
  "com.malliina" % "sbt-utils-maven" % "0.15.7",
  "com.typesafe.play" % "sbt-plugin" % "2.8.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1",
  "org.scala-js" % "sbt-scalajs" % "1.0.0",
  "ch.epfl.scala" % "sbt-bloop" % "1.3.4",
  "org.scalameta" % "sbt-scalafmt" % "2.3.0"
) map addSbtPlugin
