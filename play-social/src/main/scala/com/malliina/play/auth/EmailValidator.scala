package com.malliina.play.auth

import com.malliina.play.auth.CodeValidator._
import com.malliina.values.Email

object EmailValidator {
  def apply(conf: CodeValidationConf[Email]): EmailValidator =
    new EmailValidator(conf)

  def map[T](c: CodeValidationConf[Email])(parseUser: Verified => Either[AuthError, Email]): DiscoveringCodeValidator[Email] =
    new DiscoveringCodeValidator[Email](c.codeConf) {
      val oauth: OAuthConf[Email] = c.oauth

      override def parse(v: Verified): Either[AuthError, Email] = parseUser(v)
    }
}

class EmailValidator(conf: CodeValidationConf[Email])
  extends DiscoveringCodeValidator[Email](conf.codeConf) {
  val oauth: OAuthConf[Email] = conf.oauth

  override def parse(v: Verified): Either[AuthError, Email] =
    v.readString(EmailKey).map(Email.apply)
}
