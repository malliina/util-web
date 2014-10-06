package com.mle.play.ws

import com.mle.util.Log
import play.api.libs.iteratee.Concurrent
import play.api.mvc.RequestHeader

/**
 * @param channel channel used to push messages to the client
 * @param request the request headers from the HTTP request that initiated the WebSocket connection
 * @param user the authenticated username
 */
case class ClientInfo[T](channel: Concurrent.Channel[T], request: RequestHeader, user: String) extends SocketClient[T] with Log {
  val protocol = if (request.secure) "wss" else "ws"
  val remoteAddress = request.remoteAddress
  val describe = s"$protocol://$user@$remoteAddress"
  override val toString = describe
}