package com.malliina.play.ws

import akka.stream.scaladsl.SourceQueue

import scala.concurrent.Future

trait SocketClient[T] {
  def channel: SourceQueue[T]

  def send(message: T): Future[Unit]

  def close(): Future[Unit]
}
