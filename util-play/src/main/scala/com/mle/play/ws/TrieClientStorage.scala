package com.mle.play.ws

import com.mle.maps.TrieItemMap

/**
 * @author Michael
 */
trait TrieClientStorage extends WebSocketBase {
  protected val clientsMap = TrieItemMap.empty[Client, Unit]

  override def clients = clientsMap.keys

  override def onDisconnect(client: Client): Unit = {
    clientsMap.remove(client)
    //    if (clients.size == 0) {
    //      onNoClients()
    //    }
  }

  override def onConnect(client: Client): Unit = {
    clientsMap.put(client, ())
    //    if (clients.size == 1) {
    //      onFirstClient()
    //    }
  }

  //  def onFirstClient(): Unit = ()
  //
  //  def onNoClients(): Unit = ()
}
