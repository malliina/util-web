package com.malliina.database

import com.malliina.config.ConfigReadable

case class Conf(
  url: String,
  user: String,
  pass: String,
  driver: String,
  maxPoolSize: Int,
  autoMigrate: Boolean,
  schemaTable: String = Conf.DefaultSchemaTable
)

object Conf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"
  val DefaultSchemaTable = "flyway_schema_history"

  given ConfigReadable[Conf] = ConfigReadable.node.emap: c =>
    for
      url <- c.parse[String]("url")
      user <- c.parse[String]("user")
      pass <- c.parse[String]("pass")
      driver <- c.parse[String]("driver")
      maxPoolSize <- c.parse[Int]("maxPoolSize")
      autoMigrate <- c.parse[Boolean]("autoMigrate")
      schemaTable <- c.opt[String]("schemaTable")
    yield Conf(
      url,
      user,
      pass,
      driver,
      maxPoolSize,
      autoMigrate,
      schemaTable.getOrElse(DefaultSchemaTable)
    )
