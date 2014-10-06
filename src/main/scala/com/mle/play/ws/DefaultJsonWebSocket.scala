package com.mle.play.ws

import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{Json, Writes}
import play.api.mvc.RequestHeader

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
trait DefaultJsonWebSocket extends JsonWebSockets with Log {
  override type Client = ClientInfo[Message]
  override type AuthSuccess = String

  /**
   * TODO: Find the best way to represent a concurrent collection. Both actors and scala-stm are heavy artillery and
   * error-prone.
   */
  val clientsMap: collection.concurrent.Map[Client, Unit] = TrieMap.empty[Client, Unit]

  def clients = clientsMap.keys.toSeq

  override def newClient(user: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
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

  def unicastJson[T](user: String, message: T)(implicit writer: Writes[T]) = unicast(user, Json.toJson(message))
}
