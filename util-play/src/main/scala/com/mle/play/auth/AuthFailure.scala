package com.mle.play.auth

sealed trait AuthFailure

case object CookieMissing extends AuthFailure

case object InvalidCookie extends AuthFailure

case object InvalidCredentials extends AuthFailure
