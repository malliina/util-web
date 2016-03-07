package com.malliina.play.ws

import play.api.libs.iteratee.Concurrent

trait SocketClient[T] {
  def channel: Concurrent.Channel[T]
}
