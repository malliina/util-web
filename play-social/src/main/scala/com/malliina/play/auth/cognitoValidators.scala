package com.malliina.play.auth

import java.time.Instant

import com.malliina.play.auth.CognitoValidator._
import com.malliina.values.{Email, Username}
import play.api.Logger

object CognitoValidator extends OAuthKeys {
  private val log = Logger(getClass)

  val Access = "access"
  val Id = "id"
  val TokenUse = "token_use"
  val UserKey = "username"
  val GroupsKey = "cognito:groups"
}

case class CognitoValidation(issuer: String,
                             tokenUse: String,
                             clientIdKey: String,
                             clientId: String)

abstract class CognitoValidator[T <: TokenValue, U](keys: Seq[KeyConf], issuer: String)
    extends StaticTokenValidator[T, U](keys, issuer)

class CognitoAccessValidator(keys: Seq[KeyConf], issuer: String, clientId: String)
    extends CognitoValidator[AccessToken, CognitoUser](keys, issuer) {

  protected def toUser(verified: Verified): Either[JWTError, CognitoUser] = {
    val jwt = verified.parsed
    for {
      username <- jwt
        .readString(UserKey)
        .filterOrElse(_.nonEmpty, InvalidClaims(jwt.token, "Username must be non-empty."))
      email <- jwt.readStringOpt(EmailKey)
      groups <- jwt.readStringListOrEmpty(GroupsKey)
    } yield CognitoUser(Username(username), email.map(Email.apply), groups, verified)
  }

  override protected def validateClaims(parsed: ParsedJWT,
                                        now: Instant): Either[JWTError, ParsedJWT] =
    for {
      _ <- checkClaim(TokenUse, Access, parsed)
      _ <- checkClaim(ClientId, clientId, parsed)
    } yield parsed
}

class CognitoIdValidator(keys: Seq[KeyConf], issuer: String, val clientIds: Seq[String])
    extends CognitoValidator[IdToken, CognitoUser](keys, issuer) {
  def this(keys: Seq[KeyConf], issuer: String, clientId: String) =
    this(keys, issuer, Seq(clientId))

  override protected def toUser(verified: Verified): Either[JWTError, CognitoUser] = {
    val jwt = verified.parsed
    for {
      email <- jwt.readString(EmailKey).map(Email.apply)
      groups <- jwt.readStringListOrEmpty(GroupsKey)
    } yield CognitoUser(Username(email.email), Option(email), groups, verified)
  }

  override protected def validateClaims(parsed: ParsedJWT,
                                        now: Instant): Either[JWTError, ParsedJWT] =
    for {
      _ <- checkClaim(TokenUse, Id, parsed)
      _ <- checkContains(Aud, clientIds, parsed)
    } yield parsed
}
