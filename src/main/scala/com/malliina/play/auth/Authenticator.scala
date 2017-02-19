package com.malliina.play.auth

import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait Authenticator[AuthSuccess] {
  def authenticate(rh: RequestHeader): Future[Either[AuthFailure, AuthSuccess]]
}
