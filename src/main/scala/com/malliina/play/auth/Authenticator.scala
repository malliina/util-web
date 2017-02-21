package com.malliina.play.auth

import com.malliina.play.models.Username
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait UserAuthenticator extends Authenticator[Username]

object UserAuthenticator {
  // Can I somehow reduce the repetition with Authenticator.apply?
  def apply(auth: RequestHeader => Future[Either[AuthFailure, Username]]) =
    new UserAuthenticator {
      override def authenticate(rh: RequestHeader) = auth(rh)
    }
}

trait Authenticator[AuthSuccess] {
  def authenticate(rh: RequestHeader): Future[Either[AuthFailure, AuthSuccess]]
}

object Authenticator {
  def apply[AuthSuccess](auth: RequestHeader => Future[Either[AuthFailure, AuthSuccess]]): Authenticator[AuthSuccess] =
    new Authenticator[AuthSuccess] {
      override def authenticate(rh: RequestHeader) = auth(rh)
    }
}
