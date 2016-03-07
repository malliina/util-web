package com.malliina.play.http

import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{Cookie, Request}

class AuthRequest[A](user: String, request: Request[A], val cookie: Option[Cookie] = None)
  extends AuthenticatedRequest[A, String](user, request)
