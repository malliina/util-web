package com.malliina.play.auth

import com.malliina.values.Username
import play.api.libs.json.Json

case class Token(user: Username, series: Long, token: Long) {
  val asUnAuth = UnAuthToken(user, series, token)
}

object Token {
  implicit val json = Json.format[Token]
}
