package com.malliina.database

import com.malliina.config.ConfigReadable
import com.malliina.http.FullUrl
import com.malliina.values.{Password, Readable}

case class Conf(
  url: FullUrl,
  user: String,
  pass: Password,
  driver: String,
  maxPoolSize: Int,
  autoMigrate: Boolean,
  schemaTable: String = Conf.DefaultSchemaTable
)

object Conf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"
  val DefaultSchemaTable = "flyway_schema_history"

  given Readable[FullUrl] = FullUrl.readable
  given ConfigReadable[Conf] = ConfigReadable.node.emap: c =>
    for
      url <- c.parse[FullUrl]("url")
      user <- c.parse[String]("user")
      pass <- c.parse[Password]("pass")
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
