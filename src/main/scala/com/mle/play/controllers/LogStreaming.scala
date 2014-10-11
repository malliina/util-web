package com.mle.play.controllers

import com.mle.logbackrx.{LogEvent, RxLogback}
import play.api.libs.json.Json
import rx.lang.scala.Observable

/**
 *
 * @author mle
 */
trait LogStreaming extends Streaming {
  val jsonEvents = logEvents.map(e => Json.toJson(e))

  def appenderOpt: Option[RxLogback.EventMapping]

  def logEvents: Observable[LogEvent] = appenderOpt.map(_.logEvents) getOrElse Observable.empty
}



