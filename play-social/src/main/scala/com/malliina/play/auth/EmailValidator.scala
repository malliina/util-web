package com.malliina.play.auth

import com.malliina.play.auth.OAuthKeys.EmailKey
import com.malliina.values.Email

object EmailValidator {
  def apply(conf: CodeValidationConf[Email]): EmailValidator =
    new EmailValidator(conf)

  def map[T](
    c: CodeValidationConf[Email]
  )(parseUser: Verified => Either[JWTError, Email]): EmailValidator =
    new EmailValidator(c) {
      override def parse(v: Verified): Either[JWTError, Email] = parseUser(v)
    }
}

class EmailValidator(conf: CodeValidationConf[Email]) extends StandardOAuth(conf) {
  override def parse(v: Verified): Either[JWTError, Email] =
    v.readString(EmailKey).map(Email.apply)
}
