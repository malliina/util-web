package com.malliina.play.ws

import play.api.mvc.RequestHeader

import scala.concurrent.Future

/**
 * @author Michael
 */
trait SyncAuth extends WebSocketController {
  override def authenticateAsync(req: RequestHeader): Future[AuthSuccess] = toFuture(authenticate(req))

  def authenticate(implicit req: RequestHeader): Option[AuthSuccess]

  def toFuture[T](opt: Option[T]) = opt.fold[Future[T]](Future failed new NoSuchElementException)(Future.successful)
}
