package com.malliina.play.http

import play.api.mvc.RequestHeader

case class RequestInfo[U](user: U, request: RequestHeader)
