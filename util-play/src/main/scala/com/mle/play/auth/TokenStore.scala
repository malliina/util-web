package com.mle.play.auth

import scala.concurrent.Future

/**
 * @author Michael
 */
trait TokenStore {
  def persist(token: Token): Future[Unit]

  def remove(token: Token): Future[Unit]

  def removeAll(user: String): Future[Unit]

  def remove(user: String, series: Long): Future[Unit]

  def findToken(user: String, series: Long): Future[Option[Token]]
}
