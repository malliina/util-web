package com.mle.play.json

import play.api.libs.json.Json
import Json._
import JsonStrings._


/**
 *
 * @author mle
 */
trait JsonMessages {
  def failure(reason: String) = obj(REASON -> reason)

  val welcome = obj(EVENT -> WELCOME)
  val unauthorized = failure(ACCESS_DENIED)
}

object JsonMessages extends JsonMessages
