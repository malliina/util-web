package com.malliina.play.http

import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{Cookie, Request}

/**
  * @tparam A type of request body
  * @tparam U type of authenticated user
  */
class CookiedRequest[A, U](user: U, request: Request[A], val cookie: Option[Cookie] = None)
  extends AuthenticatedRequest[A, U](user, request)
