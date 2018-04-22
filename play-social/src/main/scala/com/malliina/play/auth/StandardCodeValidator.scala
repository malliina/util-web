package com.malliina.play.auth

import com.malliina.http.OkClient
import com.malliina.play.auth.CodeValidator._
import com.malliina.play.auth.StandardCodeValidator.log
import com.malliina.play.http.FullUrls
import com.malliina.play.models.Email
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.{BadGateway, Redirect}
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.Future

case class CodeValidationConf(brandName: String,
                              redirCall: Call,
                              handler: AuthHandler,
                              conf: AuthConf,
                              client: KeyClient,
                              extraStartParams: Map[String, String] = Map.empty,
                              extraValidateParams: Map[String, String] = Map.empty)

object CodeValidationConf {
  def google(redirCall: Call, handler: AuthHandler, conf: AuthConf, http: OkClient) = CodeValidationConf(
    "Google",
    redirCall,
    handler,
    conf,
    KeyClient.google(conf.clientId, http),
    Map(LoginHint -> "sub")
  )

  def microsoft(redirCall: Call, handler: AuthHandler, conf: AuthConf, http: OkClient) = CodeValidationConf(
    "Microsoft",
    redirCall,
    handler,
    conf,
    KeyClient.microsoft(conf.clientId, http),
    extraStartParams = Map("response_mode" -> "query"),
    extraValidateParams = Map(Scope -> scope)
  )
}

object StandardCodeValidator {
  private val log = Logger(getClass)

  def apply(conf: CodeValidationConf) = new StandardCodeValidator(conf)
}

/** A validator where the authorization and token endpoints are obtained through
  * a discovery endpoint ("knownUrl").
  *
  * @param codeConf
  */
class StandardCodeValidator(codeConf: CodeValidationConf)
  extends CodeValidator[Email] {

  val handler = codeConf.handler
  val brandName = codeConf.brandName
  val conf = codeConf.conf
  val redirCall = codeConf.redirCall
  val client = codeConf.client
  val http = client.http

  /** The initial result that initiates sign-in.
    */
  override def start(req: RequestHeader): Future[Result] = fetchConf().mapR { oauthConf =>
    val state = randomState()
    val nonce = randomState()
    val params = Map(
      ClientId -> conf.clientId,
      ResponseType -> CodeKey,
      RedirectUri -> FullUrls(redirCall, req).url,
      Scope -> scope,
      Nonce -> nonce,
      State -> state
    ) ++ codeConf.extraStartParams
    val encodedParams = params.mapValues(urlEncode)
    val url = oauthConf.authorizationEndpoint.append(s"?${stringify(encodedParams)}")
    Redirect(url.url).withSession(State -> state, Nonce -> nonce)
  }.onFail { err =>
    log.error(s"HTTP error. $err")
    BadGateway(Json.obj("message" -> "HTTP error."))
  }

  override def validate(code: Code, req: RequestHeader): Future[Either[AuthError, Email]] = {
    val params = validationParams(code, req) ++
      Map(GrantType -> AuthorizationCode) ++
      codeConf.extraValidateParams
    fetchConf().flatMapRight { oauthConf =>
      postForm[SimpleTokens](oauthConf.tokenEndpoint, params).flatMapRight { tokens =>
        client.validate(tokens.idToken).map { result =>
          for {
            verified <- result
            _ <- checkNonce(tokens.idToken, verified, req)
            email <- verified.parsed.readString(EmailKey).map(Email.apply)
          } yield email
        }
      }
    }
  }

  def checkNonce(idToken: IdToken, verified: Verified, req: RequestHeader) =
    verified.parsed.readString(Nonce).flatMap { n =>
      if (req.session.get(Nonce).contains(n)) Right(verified)
      else Left(InvalidClaims(idToken, "Nonce mismatch."))
    }

  def fetchConf(): Future[Either[OkError, AuthEndpoints]] =
    getJson[AuthEndpoints](client.knownUrl)
}
