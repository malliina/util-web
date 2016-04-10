package com.malliina.play.ws

import akka.stream.scaladsl.SourceQueue

trait SocketClient[T] {
  def channel: SourceQueue[T]
}
