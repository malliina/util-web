package com.malliina.play.controllers

import com.malliina.maps.ItemMap
import com.malliina.play.controllers.Streaming.log
import com.malliina.play.http.AuthResult
import com.malliina.play.json.SimpleCommand
import com.malliina.play.ws.{JsonWebSockets, WebSocketClient}
import play.api.Logger
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.RequestHeader
import rx.lang.scala.{Observable, Subscription}

trait Streaming extends JsonWebSockets {
  override type AuthSuccess = AuthResult
  override type Client = WebSocketClient
  val SUBSCRIBE = "subscribe"

  def subscriptions: ItemMap[WebSocketClient, Subscription]

  override def clients: Seq[Client] = subscriptions.keys

  def jsonEvents: Observable[JsValue]

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    WebSocketClient(user.user, channel, request)

  override def onMessage(msg: Message, client: Client): Boolean = {
    msg.validate[SimpleCommand].map(_.cmd match {
      case SUBSCRIBE =>
        val subscription = jsonEvents.subscribe(e => client.channel push e)
        subscriptions.put(client, subscription)
        writeLog(client, s"subscribed. Subscriptions in total: ${subscriptions.size}")
        JsSuccess
      case _ =>
        JsError //log.warn(s"Unknown message: $msg")
    }).isSuccess
  }

  override def onConnect(client: Client): Unit =
    writeLog(client, "connected")

  override def onDisconnect(client: Client): Unit = {
    subscriptions.get(client).foreach(_.unsubscribe())
    subscriptions.remove(client)
    writeLog(client, "disconnected")
  }

  protected def writeLog(client: Client, suffix: String): Unit =
    log.info(s"User: ${client.user} from: ${client.request.remoteAddress} $suffix.")
}

object Streaming {
  private val log = Logger(getClass)
}
