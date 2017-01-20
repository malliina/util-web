package com.malliina.play.ws

import akka.stream.Materializer
import play.api.mvc.RequestHeader

import scala.concurrent.Future

abstract class SyncAuth(mat: Materializer) extends WebSocketController(mat) {
  def authenticate(request: RequestHeader): Option[AuthSuccess]

  override def authenticateAsync(request: RequestHeader): Future[AuthSuccess] =
    toFuture(authenticate(request))

  def toFuture[T](opt: Option[T]) =
    opt.fold[Future[T]](Future failed new NoSuchElementException)(Future.successful)
}
