package com.malliina.database

import com.malliina.config.ConfigReadable
import com.malliina.config.ConfigReadable.ConfigOps

case class Conf(
  url: String,
  user: String,
  pass: String,
  driver: String,
  maxPoolSize: Int,
  autoMigrate: Boolean
)

object Conf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"

  given ConfigReadable[Conf] = ConfigReadable.config.emap: c =>
    for
      url <- c.parse[String]("url")
      user <- c.parse[String]("user")
      pass <- c.parse[String]("pass")
      driver <- c.parse[String]("driver")
      maxPoolSize <- c.parse[Int]("maxPoolSize")
      autoMigrate <- c.parse[Boolean]("autoMigrate")
    yield Conf(url, user, pass, driver, maxPoolSize, autoMigrate)
