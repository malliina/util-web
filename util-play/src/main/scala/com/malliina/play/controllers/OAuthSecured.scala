package com.malliina.play.controllers

import akka.stream.Materializer
import play.api.mvc.{RequestHeader, Result, Results}

abstract class OAuthSecured(oauth: OAuthControl, mat: Materializer)
  extends BaseSecurity(oauth.sessionUserKey, mat) {

  override protected def onUnauthorized(request: RequestHeader): Result =
    Results.Redirect(oauth.startOAuth)
}
