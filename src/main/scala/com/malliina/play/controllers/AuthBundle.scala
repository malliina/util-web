package com.malliina.play.controllers

import com.malliina.play.auth.{AuthFailure, Authenticator, UserAuthenticator}
import com.malliina.play.http.{AuthedRequest, Proxies}
import com.malliina.play.models.{AuthInfo, Username}
import play.api.Logger
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{Call, RequestHeader, Result, Results}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait AuthBundle[U] {
  def authenticator: Authenticator[U]

  def onUnauthorized(failure: AuthFailure): Result
}

object AuthBundle {
  private val log = Logger(getClass)

  def default[U](auth: Authenticator[U]): AuthBundle[U] =
    new AuthBundle[U] {
      override val authenticator = auth

      override def onUnauthorized(failure: AuthFailure) = {
        val rh = failure.rh
        val ip = Proxies.realAddress(rh)
        val resource = rh.path
        log warn s"Unauthorized request to '$resource' from '$ip'."
        Unauthorized
      }
    }

  def forOAuth(ctrl: OAuthControl): AuthBundle[AuthInfo] =
    oauth((req, user) => AuthedRequest(user, req), ctrl.startOAuth, ctrl.sessionUserKey)

  def oauthUser(initiateFlow: Call, sessionKey: String): AuthBundle[Username] =
    oauth[Username]((_, u) => u, initiateFlow, sessionKey)

  def oauth[T](map: (RequestHeader, Username) => T,
               initiateFlow: Call,
               sessionKey: String): AuthBundle[T] =
    new AuthBundle[T] {
      override val authenticator: Authenticator[T] = UserAuthenticator.session(sessionKey)
        .transform((r, u) => Right(map(r, u)))

      override def onUnauthorized(failure: AuthFailure) =
        Results.Redirect(initiateFlow)
    }
}
