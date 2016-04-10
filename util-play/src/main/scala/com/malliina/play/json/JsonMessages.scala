package com.malliina.play.json

import com.malliina.play.json.JsonStrings.{ACCESS_DENIED, EVENT, PING, REASON, WELCOME}
import play.api.libs.json.Json._

trait JsonMessages {
  def failure(reason: String) = obj(REASON -> reason)

  val ping = obj(EVENT -> PING)
  val welcome = obj(EVENT -> WELCOME)
  val unauthorized = failure(ACCESS_DENIED)
}

object JsonMessages extends JsonMessages
