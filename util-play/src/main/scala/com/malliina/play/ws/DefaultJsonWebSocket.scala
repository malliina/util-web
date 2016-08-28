package com.malliina.play.ws

import akka.stream.scaladsl.SourceQueue
import com.malliina.maps.{ItemMap, StmItemMap}
import com.malliina.play.models.Username
import com.malliina.play.ws.DefaultJsonWebSocket.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait DefaultJsonWebSocket extends JsonWebSockets with SyncSockets {
  override type Client = ClientInfo[Message]
  override type AuthSuccess = String

  val users: ItemMap[Client, Unit] = StmItemMap.empty[Client, Unit]

  def clientsSync: Seq[ClientInfo[JsValue]] = users.keys

  override def newClient(user: AuthSuccess, channel: SourceQueue[Message], request: RequestHeader): Client =
    ClientInfo(channel, request, user)

  /** Called when a client has connected.
    *
    * @param client the client channel, can be used to push messages to the client
    */
  override def onConnect(client: Client): Future[Unit] =
    Future.successful(users.put(client, ()))

  /** Called when a client has disconnected.
    *
    * @param client the disconnected client channel
    */
  override def onDisconnect(client: Client): Future[Unit] =
    Future.successful(users.remove(client))

  /** Sends `message` to each connection authenticated as `user`.
    *
    * @return true if at least one `user` existed and the message was sent, false otherwise
    */
  def unicast(user: Username, message: Message): Boolean = {
    val users = clientsSync.filter(_.user == user)
    implicit val ec = mat.executionContext
    Future.traverse(users)(_.channel offer message)
    val userCount = users.size
    if (userCount == 0) {
      log.warn(s"Unable to find client: $user")
    }
    userCount > 0
  }

  def unicastJson[T: Writes](user: Username, message: T) =
    unicast(user, Json.toJson(message))
}

object DefaultJsonWebSocket {
  private val log = Logger(getClass)
}
