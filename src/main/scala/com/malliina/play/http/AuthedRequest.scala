package com.malliina.play.http

import com.malliina.play.models.Username
import play.api.mvc.{AnyContent, Cookie, Request, RequestHeader}

class AuthedRequest(val user: Username, val request: RequestHeader, val cookie: Option[Cookie] = None)
  extends BaseAuthRequest[Username] {

  def fillAny(completeRequest: Request[AnyContent]): FullRequest =
    new FullRequest(user, completeRequest, cookie)

  def fill[A](fullRequest: Request[A]): CookiedRequest[A, Username] =
    new CookiedRequest[A, Username](user, fullRequest, cookie)
}

object AuthedRequest {
  def apply(user: Username, request: RequestHeader, cookie: Option[Cookie] = None) =
    new AuthedRequest(user, request, cookie)
}
