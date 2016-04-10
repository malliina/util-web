package com.malliina.play.ws

import akka.stream.scaladsl.SourceQueue
import play.api.mvc.RequestHeader

/**
  * @param channel channel used to push messages to the client
  * @param request the request headers from the HTTP request that initiated the WebSocket connection
  * @param user    the authenticated username
  * @tparam T type of message
  */
case class ClientInfo[T](channel: SourceQueue[T], request: RequestHeader, user: String) extends SocketClient[T] {
  val protocol = if (request.secure) "wss" else "ws"
  val remoteAddress = request.remoteAddress
  val describe = s"$protocol://$user@$remoteAddress"
  override val toString = describe
}
