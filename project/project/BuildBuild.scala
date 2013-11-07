import sbt._

object BuildBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    Keys.scalaVersion := "2.10.3"
  ) ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.github.mpeltonen" % "sbt-idea" % "1.5.1",
    "com.typesafe.sbt" % "sbt-pgp" % "0.8"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}