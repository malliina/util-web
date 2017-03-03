package com.malliina.play.ws

import akka.stream.scaladsl.SourceQueue
import com.malliina.play.http.Proxies
import com.malliina.play.models.Username
import play.api.mvc.RequestHeader

/**
  * @param channel channel used to push messages to the client
  * @param rh the request headers from the HTTP request that initiated the WebSocket connection
  * @param user    the authenticated username
  * @tparam T type of message
  */
case class ClientInfo[T](channel: SourceQueue[T], rh: RequestHeader, user: Username)
  extends SocketClient[T] {

  val protocol = if (Proxies.isSecure(rh)) "wss" else "ws"
  val remoteAddress = Proxies.realAddress(rh)
  val describe = s"$protocol://$user@$remoteAddress"
  override val toString = describe
}
