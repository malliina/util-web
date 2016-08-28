package com.malliina.play.controllers

import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

trait OAuthSecured extends OAuthControl with BaseSecurity {

  override def authenticate(request: RequestHeader): Future[Option[AuthedRequest]] =
    authenticateFromSession(request) map (_.map(lift(_, request)))

  override def authenticateFromSession(request: RequestHeader): Future[Option[Username]] =
    fut(request.session.get(sessionUserKey).map(Username.apply))

  protected override def onUnauthorized(headers: RequestHeader): Result =
    Redirect(startOAuth)
}
