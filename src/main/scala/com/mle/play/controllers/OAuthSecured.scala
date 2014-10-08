package com.mle.play.controllers

import play.api.mvc.{RequestHeader, Result}

/**
 * @author Michael
 */
trait OAuthSecured extends OAuthControl with BaseSecurity {

  override def authenticate(implicit request: RequestHeader): Option[AuthResult] =
    authenticateFromSession(request) map lift

  override def authenticateFromSession(implicit request: RequestHeader): Option[String] =
    request.session.get(sessionUserKey)

  protected override def onUnauthorized(implicit headers: RequestHeader): Result = Redirect(startOAuth)
}
