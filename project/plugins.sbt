scalaVersion := "2.12.11"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.0.0",
  "com.typesafe.play" % "sbt-plugin" % "2.8.3",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0",
  "org.scala-js" % "sbt-scalajs" % "1.1.0",
  "ch.epfl.scala" % "sbt-bloop" % "1.4.4",
  "org.scalameta" % "sbt-scalafmt" % "2.4.2"
) map addSbtPlugin
