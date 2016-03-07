package com.malliina.play.auth

import play.api.libs.json.Json

case class Token(user: String, series: Long, token: Long) {
  val asUnAuth = UnAuthToken(user, series, token)
}

object Token {
  implicit val json = Json.format[Token]
}
