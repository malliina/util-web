package com.malliina.play.models

import play.api.mvc.RequestHeader

class AuthRequest(val user: Username, val rh: RequestHeader) extends AuthInfo
