package com.malliina.play.auth

import com.malliina.play.models.Username

import scala.concurrent.Future

trait TokenStore {
  def persist(token: Token): Future[Unit]

  def remove(token: Token): Future[Unit]

  def removeAll(user: Username): Future[Unit]

  def remove(user: Username, series: Long): Future[Unit]

  def findToken(user: Username, series: Long): Future[Option[Token]]
}
