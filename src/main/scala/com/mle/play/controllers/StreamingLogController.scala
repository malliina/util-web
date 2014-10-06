package com.mle.play.controllers

import com.mle.logbackrx.LogEvent
import com.mle.play.json.SimpleCommand
import com.mle.play.ws.{JsonWebSockets, SocketClient}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.mvc.WebSocket.FrameFormatter
import rx.lang.scala.{Observable, Subscription}

import scala.collection.concurrent.TrieMap

/**
 *
 * @author mle
 */
trait StreamingLogController extends JsonWebSockets {
  override type Client = WebSocketClient
  override type AuthResult = String

  def logEvents: Observable[LogEvent]

  private val jsonLogEvents = logEvents.map(e => Json.toJson(e))

  private val subscriptions = TrieMap.empty[WebSocketClient, Subscription]


  val SUBSCRIBE = "subscribe"

  def openLogSocket = ws(FrameFormatter.jsonFrame)

  override def newClient(user: AuthResult, channel: Channel[Message])(implicit request: RequestHeader): Client =
    WebSocketClient(user, channel, request)

  override def onMessage(msg: Message, client: Client): Unit = {
    msg.validate[SimpleCommand].map(_.cmd match {
      case SUBSCRIBE =>
        val subscription = jsonLogEvents.subscribe(e => client.channel push e)
        subscriptions += (client -> subscription)
        writeLog(client, s"subscribed. Subscriptions in total: ${subscriptions.size}")
      case _ => log.warn(s"Unknown message: $msg")
    })
  }

  override def onConnect(client: Client): Unit =
    writeLog(client, "connected")

  override def onDisconnect(client: Client): Unit = {
    subscriptions.get(client).foreach(_.unsubscribe())
    subscriptions -= client
    writeLog(client, "disconnected")
  }

  //  protected def broadcast(message: String) =
  //    subscriptions.keys.foreach(client => client.channel push Json.toJson(message))

  private def writeLog(client: Client, suffix: String): Unit =
    log.info(s"User: ${client.user} from: ${client.request.remoteAddress} $suffix.")

  //  trait WebSocketClientBase {
  //    def user: String
  //
  //    def channel: Channel[JsValue]
  //  }

}

case class WebSocketClient(user: String, channel: Channel[JsValue], request: RequestHeader) extends SocketClient[JsValue]




