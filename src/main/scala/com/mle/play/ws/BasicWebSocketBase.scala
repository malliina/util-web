package com.mle.play.ws

/**
 * @author Michael
 */
trait BasicWebSocketBase extends WebSocketBase {
  override type AuthResult = String
}