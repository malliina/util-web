package com.malliina.play.http

import play.api.mvc.Cookie

case class AuthResult(user: String, cookie: Option[Cookie] = None)
