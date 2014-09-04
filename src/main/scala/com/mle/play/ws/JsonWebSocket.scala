package com.mle.play.ws

import java.util.concurrent.ConcurrentHashMap

import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.{Call, RequestHeader}

import scala.collection.JavaConversions._

/**
 * @author Michael
 */
trait JsonWebSocket extends WebSocketController with Log {
  override type Message = JsValue
  override type Client = ClientInfo[Message]

  /**
   * TODO: Find the best way to represent a concurrent collection. Both actors and scala-stm are heavy artillery and
   * error-prone.
   */
  val clientsMap: collection.concurrent.Map[Client, Unit] = new ConcurrentHashMap[Client, Unit]()

  def clients = clientsMap.keys

  /**
   * Implement this like `routes.YourController.openSocket()`.
   *
   * @return
   */
  def openSocketCall: Call

  def openSocket = ws3(FrameFormatter.jsonFrame)

  def wsUrl(implicit request: RequestHeader): String = openSocketCall.webSocketURL(request.secure)

  override def newClient(user: String, channel: Channel[Message])(implicit request: RequestHeader): Client =
    ClientInfo(channel, request, user)

  /**
   * Called when a client has connected.
   *
   * @param client the client channel, can be used to push messages to the client
   */
  override def onConnect(client: Client): Unit = {
    clientsMap += client ->()
  }

  /**
   * Called when a client has disconnected.
   *
   * @param client the disconnected client channel
   */
  override def onDisconnect(client: Client): Unit = {
    clientsMap -= client
  }

  /**
   * Sends `message` to each connection authenticated as `user`.
   *
   * @return true if at least one `user` existed and the message was sent, false otherwise
   */
  def unicast(user: String, message: Message): Boolean = {
    val users = clients.filter(_.user == user)
    users.foreach(_.channel push message)
    val userCount = users.size
    if (userCount == 0) {
      log.warn(s"Unable to find client: $user")
    }
    userCount > 0
  }

  def broadcast(message: Message) = {
    // ?
    clients.foreach(_.channel push message)
  }
}
