package com.malliina.play.ws

import com.malliina.maps.TrieItemMap

trait TrieClientStorage extends SyncSockets {
  protected val clientsMap = TrieItemMap.empty[Client, Unit]

  override def clientsSync: Seq[Client] = clientsMap.keys

  override def onDisconnectSync(client: Client): Unit =
    clientsMap.remove(client)

  override def onConnectSync(client: Client): Unit =
    clientsMap.put(client, ())
}
