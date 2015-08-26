package com.mle.play.json

import com.mle.play.json.JsonStrings._
import play.api.libs.json.Json._


/**
 *
 * @author mle
 */
trait JsonMessages {
  def failure(reason: String) = obj(REASON -> reason)

  val ping = obj(EVENT -> PING)
  val welcome = obj(EVENT -> WELCOME)
  val unauthorized = failure(ACCESS_DENIED)
}

object JsonMessages extends JsonMessages
