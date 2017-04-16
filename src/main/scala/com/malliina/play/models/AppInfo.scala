package com.malliina.play.models

import play.api.libs.json.Json

case class AppInfo(name: String, version: String, hash: String)

object AppInfo {
  implicit val json = Json.format[AppInfo]
}
