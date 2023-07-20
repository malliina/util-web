package com.malliina.database

import com.malliina.config.ConfigReadable
import com.malliina.values.ErrorMessage
import com.typesafe.config.Config

case class Conf(
  url: String,
  user: String,
  pass: String,
  driver: String,
  maxPoolSize: Int,
  autoMigrate: Boolean
)

implicit class ConfigOps(c: Config) extends AnyVal:
  def read[T](key: String)(implicit r: ConfigReadable[T]): Either[ErrorMessage, T] =
    r.read(key, c)

object Conf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"

  implicit val config: ConfigReadable[Conf] = ConfigReadable.config.emap { c =>
    for
      url <- c.read[String]("url")
      user <- c.read[String]("user")
      pass <- c.read[String]("pass")
      driver <- c.read[String]("driver")
      maxPoolSize <- c.read[Int]("maxPoolSize")
      autoMigrate <- c.read[Boolean]("autoMigrate")
    yield Conf(url, user, pass, driver, maxPoolSize, autoMigrate)
  }
