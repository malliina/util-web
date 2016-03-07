package com.malliina.play.json

import play.api.libs.json.Json

case class SimpleCommand(cmd: String)

object SimpleCommand {
  implicit val json = Json.format[SimpleCommand]
}
