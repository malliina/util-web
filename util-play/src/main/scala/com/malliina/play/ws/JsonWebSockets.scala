package com.malliina.play.ws

import com.malliina.play.json.JsonMessages
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket
import play.api.mvc.WebSocket.FrameFormatter
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

/**
 * @author Michael
 */
trait JsonWebSockets extends WebSocketController {
  override type Message = JsValue
  // prevents connections being dropped after 30s of inactivity; i don't know how to modify that timeout
  val pinger = Observable.interval(20.seconds).subscribe(_ => broadcast(JsonMessages.ping))

  def openSocket: WebSocket[Message, Message] = ws(FrameFormatter.jsonFrame)

  def openSocket2: WebSocket[Message, Message] = ws2(FrameFormatter.jsonFrame)
}
