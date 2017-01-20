package com.malliina.play.ws

import com.malliina.services.AsyncStore

import scala.concurrent.Future

trait AsyncWebSocket extends WebSocketBase {
  def storage: AsyncStore[Client]

  override def clients: Future[Seq[Client]] = storage.items

  override def onConnect(client: Client): Future[Unit] = storage.add(client)

  override def onDisconnect(client: Client): Future[Unit] = storage.remove(client)
}
