package com.mle.play.http

import play.api.mvc.RequestHeader

/**
 *
 * @author mle
 */
case class RequestInfo(user: String, request: RequestHeader)
