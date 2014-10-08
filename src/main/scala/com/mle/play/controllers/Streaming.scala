package com.mle.play.controllers

import com.mle.play.json.SimpleCommand
import com.mle.play.ws.{JsonWebSockets, WebSocketClient}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import rx.lang.scala.{Observable, Subscription}

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait Streaming extends JsonWebSockets {
  override type AuthSuccess = AuthResult
  override type Client = WebSocketClient
  val SUBSCRIBE = "subscribe"

  val subscriptions = TrieMap.empty[WebSocketClient, Subscription]

  override def clients: Seq[Client] = subscriptions.keys.toSeq

  def jsonEvents: Observable[JsValue]

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    WebSocketClient(user.user, channel, request)

  override def onMessage(msg: Message, client: Client): Unit = {
    msg.validate[SimpleCommand].map(_.cmd match {
      case SUBSCRIBE =>
        val subscription = jsonEvents.subscribe(e => client.channel push e)
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

  protected def writeLog(client: Client, suffix: String): Unit =
    log.info(s"User: ${client.user} from: ${client.request.remoteAddress} $suffix.")
}