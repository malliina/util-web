package com.malliina.play.auth

import com.malliina.play.auth.CodeValidator._
import com.malliina.values.Email

object EmailValidator {
  def apply(conf: CodeValidationConf[Email]): EmailValidator =
    new EmailValidator(conf)

  def map[T](c: CodeValidationConf[Email])(parseUser: Verified => Either[AuthError, Email]): EmailValidator =
    new EmailValidator(c) {
      override def parse(v: Verified): Either[AuthError, Email] = parseUser(v)
    }
}

class EmailValidator(conf: CodeValidationConf[Email])
  extends StandardOAuth(conf) {

  override def parse(v: Verified): Either[AuthError, Email] =
    v.readString(EmailKey).map(Email.apply)
}