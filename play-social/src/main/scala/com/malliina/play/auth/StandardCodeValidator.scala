package com.malliina.play.auth

import com.malliina.http.OkClient
import com.malliina.play.auth.CodeValidator._
import com.malliina.values.Email
import play.api.mvc.{Call, RequestHeader, Result}

case class AuthCodeConf(brandName: String,
                        redirCall: Call,
                        conf: AuthConf,
                        client: KeyClient,
                        extraStartParams: Map[String, String] = Map.empty,
                        extraValidateParams: Map[String, String] = Map.empty)

case class CodeValidationConf(handler: AuthHandler, codeConf: AuthCodeConf) {
  def brandName = codeConf.brandName

  def conf = codeConf.conf

  def client = codeConf.client

  def redirCall = codeConf.redirCall

  def extraStartParams = codeConf.extraStartParams

  def extraValidateParams = codeConf.extraValidateParams
}

object CodeValidationConf {
  def google(redirCall: Call, handler: AuthHandler, conf: AuthConf, http: OkClient) =
    CodeValidationConf(
      handler,
      AuthCodeConf(
        "Google",
        redirCall,
        conf,
        KeyClient.google(conf.clientId, http),
        Map.empty
      )
    )

  def microsoft(redirCall: Call, handler: AuthHandler, conf: AuthConf, http: OkClient) =
    CodeValidationConf(
      handler,
      AuthCodeConf(
        "Microsoft",
        redirCall,
        conf,
        KeyClient.microsoft(conf.clientId, http),
        extraStartParams = Map("response_mode" -> "query"),
        extraValidateParams = Map(Scope -> scope)
      )
    )
}

object StandardCodeValidator {
  def apply(conf: CodeValidationConf) = new StandardCodeValidator(conf)

  def map[T](c: CodeValidationConf)(parse: Verified => Either[AuthError, Email]) =
    new DiscoveringCodeValidator(c.codeConf) {
      override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader): Result =
        c.handler.resultFor(outcome.flatMap(parse), req)
    }
}

class StandardCodeValidator(conf: CodeValidationConf) extends DiscoveringCodeValidator(conf.codeConf) {
  override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader): Result = {
    val emailOutcome = for {
      validated <- outcome
      email <- validated.readString(EmailKey).map(Email.apply)
    } yield email
    conf.handler.resultFor(emailOutcome, req)
  }
}

object GoogleCodeValidator {
  val EmailVerified = "email_verified"

  def apply(code: CodeValidationConf): DiscoveringCodeValidator = StandardCodeValidator.map(code) { validated =>
    for {
      _ <- validated.readBoolean(GoogleCodeValidator.EmailVerified).filterOrElse(_ == true, InvalidClaims(validated.token, "Email not verified."))
      email <- validated.readString(EmailKey).map(Email.apply)
    } yield email
  }
}
