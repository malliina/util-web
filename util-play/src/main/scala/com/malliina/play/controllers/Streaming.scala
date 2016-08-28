package com.malliina.play.controllers

import akka.stream.scaladsl.SourceQueue
import com.malliina.maps.ItemMap
import com.malliina.play.controllers.Streaming.log
import com.malliina.play.http.AuthedRequest
import com.malliina.play.json.SimpleCommand
import com.malliina.play.models.Username
import com.malliina.play.ws.{JsonSocketClient, JsonWebSockets, SyncSockets}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.RequestHeader
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.Future

trait Streaming extends JsonWebSockets with SyncSockets {
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
          (err: Throwable) => log.error(s"WebSocket error for user ${client.user} from ${client.request.remoteAddress}", err),
          () => ())
        subscriptions.put(client, subscription)
        writeLog(client, s"subscribed. Subscriptions in total: ${subscriptions.size}")
        JsSuccess
      case _ =>
        JsError //log.warn(s"Unknown message: $msg")
    }).isSuccess
  }

  override def onConnect(client: Client): Future[Unit] =
    Future.successful(writeLog(client, "connected"))

  override def onDisconnect(client: Client): Future[Unit] = Future.successful {
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
