package com.malliina.play.controllers

import com.malliina.logbackrx.{LogEvent, RxLogback}
import play.api.libs.json.Json
import rx.lang.scala.Observable

trait LogStreaming extends Streaming {
  lazy val jsonEvents = logEvents.map(e => Json.toJson(e))

  def appender: RxLogback.EventMapping

  def logEvents: Observable[LogEvent] = appender.logEvents
}
