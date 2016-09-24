package com.malliina.play.ws

import akka.stream.Materializer
import com.malliina.play.json.JsonMessages
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.MessageFlowTransformer
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

abstract class JsonWebSockets(mat: Materializer) extends WebSocketController(mat) {
  override type Message = JsValue
  // prevents connections being dropped after 30s of inactivity; i don't know how to modify that timeout
  val pinger = Observable.interval(20.seconds).subscribe(_ => broadcast(JsonMessages.ping))

  def openSocket = ws(MessageFlowTransformer.jsonMessageFlowTransformer)

  def openSocket2 = ws2(MessageFlowTransformer.jsonMessageFlowTransformer)
}
