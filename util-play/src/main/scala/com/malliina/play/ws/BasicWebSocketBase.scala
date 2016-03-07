package com.malliina.play.ws

trait BasicWebSocketBase extends WebSocketBase {
  override type AuthSuccess = String
}