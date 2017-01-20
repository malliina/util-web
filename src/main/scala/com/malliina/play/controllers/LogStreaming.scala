package com.malliina.play.controllers

import akka.stream.Materializer
import com.malliina.logbackrx.{LogEvent, RxLogback}
import play.api.libs.json.Json
import rx.lang.scala.Observable

abstract class LogStreaming(mat: Materializer) extends Streaming(mat) {
  lazy val jsonEvents = logEvents.map(e => Json.toJson(e))

  def appender: RxLogback.EventMapping

  def logEvents: Observable[LogEvent] = appender.logEvents
}
