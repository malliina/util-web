package com.malliina.play.ws

import akka.stream.QueueOfferResult
import akka.stream.scaladsl.SourceQueue
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait WebSocketBase {
  type Message
  type Client <: SocketClient[Message]
  type AuthSuccess

  /**
    *
    * @return the currently connected clients
    */
  def clients: Future[Seq[Client]]

  def newClient(authResult: AuthSuccess, channel: SourceQueue[Message])(implicit request: RequestHeader): Client

  def wsUrl(implicit request: RequestHeader): String

  /** Called when the client sends a message to the server.
    *
    * @param msg    the message
    * @param client the client that sent the message
    * @return true if the message was handled, false otherwise
    */
  def onMessage(msg: Message, client: Client): Boolean = false

  /** Called when a client has been created. Note however that messages cannot yet be sent to the client.
    *
    * @param client the client channel, can be used to push messages to the client
    */
  def onConnect(client: Client): Future[Unit]

  /** Called when a client has disconnected.
    *
    * @param client the disconnected client channel
    */
  def onDisconnect(client: Client): Future[Unit]

  def broadcast(message: Message): Future[Seq[QueueOfferResult]]
}
