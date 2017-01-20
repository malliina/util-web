package com.malliina.play.ws

import scala.concurrent.Future

trait SyncSockets extends WebSocketBase {
  def clientsSync: Seq[Client]

  def onConnectSync(client: Client): Unit

  def onDisconnectSync(client: Client): Unit

  override def clients: Future[Seq[Client]] = Future.successful(clientsSync)

  override def onConnect(client: Client): Future[Unit] = Future.successful(onConnectSync(client))

  override def onDisconnect(client: Client): Future[Unit] = Future.successful(onDisconnectSync(client))
}
