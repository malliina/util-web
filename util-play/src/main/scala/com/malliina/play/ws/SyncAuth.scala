package com.malliina.play.ws

import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait SyncAuth extends WebSocketController {
  override def authenticateAsync(request: RequestHeader): Future[AuthSuccess] =
    toFuture(authenticate(request))

  def authenticate(request: RequestHeader): Option[AuthSuccess]

  def toFuture[T](opt: Option[T]) = opt.fold[Future[T]](Future failed new NoSuchElementException)(Future.successful)
}
