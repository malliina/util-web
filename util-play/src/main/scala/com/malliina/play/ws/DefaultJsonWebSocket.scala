package com.malliina.play.ws

import akka.stream.scaladsl.SourceQueue
import com.malliina.maps.{ItemMap, StmItemMap}
import com.malliina.play.ws.DefaultJsonWebSocket.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.RequestHeader
import scala.concurrent.Future

trait DefaultJsonWebSocket extends JsonWebSockets {
  override type Client = ClientInfo[Message]
  override type AuthSuccess = String

  val users: ItemMap[Client, Unit] = StmItemMap.empty[Client, Unit]

  def clients: Seq[ClientInfo[JsValue]] = users.keys

  override def newClient(user: AuthSuccess, channel: SourceQueue[Message])(implicit request: RequestHeader): Client =
    ClientInfo(channel, request, user)

  /** Called when a client has connected.
    *
    * @param client the client channel, can be used to push messages to the client
    */
  override def onConnect(client: Client): Unit = {
    users.put(client, ())
  }

  /** Called when a client has disconnected.
    *
    * @param client the disconnected client channel
    */
  override def onDisconnect(client: Client): Unit = {
    users.remove(client)
  }

  /** Sends `message` to each connection authenticated as `user`.
    *
    * @return true if at least one `user` existed and the message was sent, false otherwise
    */
  def unicast(user: String, message: Message): Boolean = {
    val users = clients.filter(_.user == user)
    implicit val ec = mat.executionContext
    Future.traverse(users)(_.channel offer message)
    val userCount = users.size
    if (userCount == 0) {
      log.warn(s"Unable to find client: $user")
    }
    userCount > 0
  }

  def unicastJson[T](user: String, message: T)(implicit writer: Writes[T]) =
    unicast(user, Json.toJson(message))
}

object DefaultJsonWebSocket {
  private val log = Logger(getClass)
}
