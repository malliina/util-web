package com.malliina.play.controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

/**
 * @author Michael
 */
trait OAuthSecured extends OAuthControl with BaseSecurity {

  override def authenticate(implicit request: RequestHeader): Future[Option[AuthResult]] =
    authenticateFromSession(request) map (_.map(lift))

  override def authenticateFromSession(implicit request: RequestHeader): Future[Option[String]] =
    fut(request.session.get(sessionUserKey))

  protected override def onUnauthorized(implicit headers: RequestHeader): Result = Redirect(startOAuth)
}
