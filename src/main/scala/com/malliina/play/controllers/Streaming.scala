package com.malliina.play.controllers

import akka.stream.Materializer
import akka.stream.scaladsl.SourceQueue
import com.malliina.maps.ItemMap
import com.malliina.play.controllers.Streaming.log
import com.malliina.play.http.{AuthedRequest, Proxies}
import com.malliina.play.json.SimpleCommand
import com.malliina.play.models.Username
import com.malliina.play.ws.{JsonSocketClient, JsonWebSockets, SyncSockets}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.RequestHeader
import rx.lang.scala.{Observable, Subscription}

abstract class Streaming(mat: Materializer) extends JsonWebSockets(mat) with SyncSockets {
  override type AuthSuccess = AuthedRequest
  override type Client = JsonSocketClient[Username]
  val SUBSCRIBE = "subscribe"

  def subscriptions: ItemMap[Client, Subscription]

  override def clientsSync: Seq[Client] = subscriptions.keys

  def jsonEvents: Observable[JsValue]

  override def newClient(user: AuthSuccess, channel: SourceQueue[Message], request: RequestHeader): Client =
    JsonSocketClient(user.user, channel, request)

  override def onMessage(msg: Message, client: Client): Boolean = {
    msg.validate[SimpleCommand].map(_.cmd match {
      case SUBSCRIBE =>
        val subscription = jsonEvents.subscribe(
          e => client.channel.offer(e),
          (err: Throwable) => log.error(s"WebSocket error for user ${client.user} from ${Proxies.realAddress(client.request)}", err),
          () => ())
        subscriptions.put(client, subscription)
        writeLog(client, s"subscribed. Subscriptions in total: ${subscriptions.size}")
        JsSuccess
      case _ =>
        JsError //log.warn(s"Unknown message: $msg")
    }).isSuccess
  }

  override def onConnectSync(client: Client): Unit =
    writeLog(client, "connected")

  override def onDisconnectSync(client: Client): Unit = {
    subscriptions.get(client).foreach(_.unsubscribe())
    subscriptions.remove(client)
    writeLog(client, "disconnected")
  }

  protected def writeLog(client: Client, suffix: String): Unit =
    log.info(s"User: ${client.user} from: ${Proxies.realAddress(client.request)} $suffix.")
}

object Streaming {
  private val log = Logger(getClass)
}
