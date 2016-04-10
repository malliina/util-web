package com.malliina.play.http

import play.api.mvc.RequestHeader

case class RequestInfo(user: String, request: RequestHeader)
