package com.malliina.play.ws

/**
 * @author Michael
 */
trait BasicWebSocketBase extends WebSocketBase {
  override type AuthSuccess = String
}