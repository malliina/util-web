package com.malliina.play.json

import com.malliina.play.json.JsonStrings.{AccessDenied, Event, Ping, Reason, Welcome}
import play.api.libs.json.Json._

trait JsonMessages {
  def failure(reason: String) = obj(Reason -> reason)

  val ping = obj(Event -> Ping)
  val welcome = obj(Event -> Welcome)
  val unauthorized = failure(AccessDenied)
}

object JsonMessages extends JsonMessages
