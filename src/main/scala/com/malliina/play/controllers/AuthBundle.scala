package com.malliina.play.controllers

import com.malliina.play.auth.{AuthFailure, Authenticator, UserAuthenticator}
import com.malliina.play.http.Proxies
import com.malliina.play.models.Username
import play.api.Logger
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{Call, Result, Results}

trait AuthBundle[U] {
  def authenticator: Authenticator[U]

  def onUnauthorized(failure: AuthFailure): Result
}

object AuthBundle {
  private val log = Logger(getClass)

  def default[U](auth: Authenticator[U]) = new AuthBundle[U] {
    override val authenticator = auth

    override def onUnauthorized(failure: AuthFailure) = {
      val rh = failure.rh
      val ip = Proxies.realAddress(rh)
      val resource = rh.path
      log warn s"Unauthorized request to '$resource' from '$ip'."
      Unauthorized
    }
  }

  def oauth(initiateFlow: Call, sessionKey: String): AuthBundle[Username] =
    new AuthBundle[Username] {
      override val authenticator = UserAuthenticator.session(sessionKey)

      override def onUnauthorized(failure: AuthFailure) =
        Results.Redirect(initiateFlow)
    }
}
