package com.mle.play.controllers

import com.mle.logbackrx.{LogEvent, RxLogback}
import play.api.libs.json.Json
import rx.lang.scala.Observable

/**
 *
 * @author mle
 */
trait LogStreaming extends Streaming {
  lazy val jsonEvents = logEvents.map(e => Json.toJson(e))

  def appender: RxLogback.EventMapping

  def logEvents: Observable[LogEvent] = appender.logEvents
}



