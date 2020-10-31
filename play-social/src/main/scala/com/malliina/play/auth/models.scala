package com.malliina.play.auth

import java.text.ParseException
import java.time.Instant

import com.malliina.http.FullUrl
import com.malliina.json.PrimitiveFormats.durationFormat
import com.malliina.values.{
  AccessToken,
  Email,
  ErrorMessage,
  IdToken,
  RefreshToken,
  StringCompanion,
  TokenValue,
  Username,
  WrappedString
}
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}

import scala.concurrent.duration.{Duration, DurationLong}

case class ClientId(value: String) extends AnyVal with WrappedString
object ClientId extends StringCompanion[ClientId]

case class ClientSecret(value: String) extends AnyVal with WrappedString
object ClientSecret extends StringCompanion[ClientSecret]

case class Issuer(value: String) extends AnyVal with WrappedString
object Issuer extends StringCompanion[Issuer]

case class Code(code: String) extends AnyVal with WrappedString {
  override def value = code
}
object Code extends StringCompanion[Code]

case class AuthConf(clientId: ClientId, clientSecret: ClientSecret)

object AuthConf {
  def google = AuthConfReader.env.google
}

object RequestToken {
  val Key = "request_token"
}

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class CognitoTokens(accessToken: AccessToken, idToken: IdToken, refreshToken: RefreshToken)

object CognitoTokens {
  implicit val json: Format[CognitoTokens] = (
    (JsPath \ "access_token").format[AccessToken] and
      (JsPath \ "id_token").format[IdToken] and
      (JsPath \ "refresh_token").format[RefreshToken]
  )(CognitoTokens.apply, unlift(CognitoTokens.unapply))
}

case class GitHubTokens(accessToken: AccessToken, tokenType: String)

object GitHubTokens {
  implicit val json: Format[GitHubTokens] = (
    (JsPath \ "access_token").format[AccessToken] and
      (JsPath \ "token_type").formatWithDefault[String]("dummy")
  )(GitHubTokens.apply, unlift(GitHubTokens.unapply))
}

case class GitHubEmail(
  email: Email,
  primary: Boolean,
  verified: Boolean,
  visibility: Option[String]
)

object GitHubEmail {
  implicit val json = Json.format[GitHubEmail]
}

trait OpenIdConf {
  def jwksUri: FullUrl
}

case class SimpleOpenIdConf(jwksUri: FullUrl) extends OpenIdConf

object SimpleOpenIdConf {
  implicit val reader = Reads[SimpleOpenIdConf] { json =>
    (json \ "jwks_uri").validate[FullUrl].map(apply)
  }
}

case class AuthEndpoints(authorizationEndpoint: FullUrl, tokenEndpoint: FullUrl, jwksUri: FullUrl)
  extends OpenIdConf

object AuthEndpoints {
  implicit val reader: Reads[AuthEndpoints] = (
    (JsPath \ "authorization_endpoint").read[FullUrl] and
      (JsPath \ "token_endpoint").read[FullUrl] and
      (JsPath \ "jwks_uri").read[FullUrl]
  )(AuthEndpoints.apply _)
}

case class MicrosoftOAuthConf(
  authorizationEndpoint: FullUrl,
  tokenEndpoint: FullUrl,
  jwksUri: FullUrl,
  endSessionEndpoint: FullUrl,
  scopesSupported: Seq[String],
  issuer: Issuer,
  claimsSupported: Seq[String]
) extends OpenIdConf

object MicrosoftOAuthConf {
  implicit val reader: Reads[MicrosoftOAuthConf] = (
    (JsPath \ "authorization_endpoint").read[FullUrl] and
      (JsPath \ "token_endpoint").read[FullUrl] and
      (JsPath \ "jwks_uri").read[FullUrl] and
      (JsPath \ "end_session_endpoint").read[FullUrl] and
      (JsPath \ "scopes_supported").read[Seq[String]] and
      (JsPath \ "issuer").read[Issuer] and
      (JsPath \ "claims_supported").read[Seq[String]]
  )(MicrosoftOAuthConf.apply _)
}

trait TokenSet {
  def idToken: IdToken
}

case class SimpleTokens(idToken: IdToken)

object SimpleTokens {
  implicit val reader = Reads[SimpleTokens] { json =>
    (json \ "id_token").validate[IdToken].map(SimpleTokens(_))
  }
}

/** https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-protocols-oauth-code
  *
  * @param idToken      only returned if scope "openid" is requested
  * @param refreshToken only returned if scope "offline_access" is requested
  * @param tokenType    Bearer
  * @param expiresIn    seconds
  */
case class MicrosoftTokens(
  idToken: IdToken,
  accessToken: Option[AccessToken],
  refreshToken: Option[RefreshToken],
  tokenType: Option[String],
  expiresIn: Option[Duration],
  scope: Option[String]
) extends TokenSet

object MicrosoftTokens {
  implicit val json: Format[MicrosoftTokens] = (
    (JsPath \ "id_token").format[IdToken] and
      (JsPath \ "access_token").formatNullable[AccessToken] and
      (JsPath \ "refresh_token").formatNullable[RefreshToken] and
      (JsPath \ "token_type").formatNullable[String] and
      (JsPath \ "expires_in").formatNullable[Duration] and
      (JsPath \ "scope").formatNullable[String]
  )(MicrosoftTokens.apply, unlift(MicrosoftTokens.unapply))
}

case class GoogleTokens(
  idToken: IdToken,
  accessToken: AccessToken,
  expiresIn: Duration,
  tokenType: String
) extends TokenSet

object GoogleTokens {
  implicit val json: Format[GoogleTokens] = (
    (JsPath \ "id_token").format[IdToken] and
      (JsPath \ "access_token").format[AccessToken] and
      (JsPath \ "expires_in").format[Duration] and
      (JsPath \ "token_type").format[String]
  )(GoogleTokens.apply, unlift(GoogleTokens.unapply))
}

case class FacebookTokens(accessToken: AccessToken, tokenType: String, expiresIn: Duration)

object FacebookTokens {
  implicit val json: Format[FacebookTokens] = (
    (JsPath \ "access_token").format[AccessToken] and
      (JsPath \ "token_type").format[String] and
      (JsPath \ "expires_in").format[Duration]
  )(FacebookTokens.apply, unlift(FacebookTokens.unapply))
}

case class TwitterTokens(
  oauthToken: AccessToken,
  oauthTokenSecret: String,
  oauthCallbackConfirmed: Boolean
)

object TwitterTokens {
  def fromString(in: String) = {
    val map = parseMap(in)
    for {
      ot <- map.get("oauth_token").map(AccessToken.apply)
      ots <- map.get("oauth_token_secret")
      c <- map.get("oauth_callback_confirmed")
    } yield TwitterTokens(ot, ots, c == "true")
  }

  def parseMap(in: String) = in
    .split("&")
    .toList
    .flatMap { kv =>
      val parts = kv.split("=")
      if (parts.length == 2) {
        val Array(k, v) = parts
        Option(k -> v)
      } else {
        None
      }
    }
    .toMap
}

case class TwitterAccess(oauthToken: AccessToken, oauthTokenSecret: String)

object TwitterAccess {
  def fromString(in: String) = {
    val map = TwitterTokens.parseMap(in)
    for {
      ot <- map.get("oauth_token").map(AccessToken.apply)
      ots <- map.get("oauth_token_secret")
    } yield TwitterAccess(ot, ots)
  }
}

case class TwitterUser(id: String, name: String, screenName: String, email: Option[Email])

object TwitterUser {
  implicit val json: Format[TwitterUser] = (
    (JsPath \ "id_str").format[String] and
      (JsPath \ "name").format[String] and
      (JsPath \ "screen_name").format[String] and
      (JsPath \ "email").formatNullable[Email]
  )(TwitterUser.apply, unlift(TwitterUser.unapply))
}

case class EmailResponse(email: Email)

object EmailResponse {
  implicit val json = Json.format[EmailResponse]
}

trait Readable[R] {
  def read(key: String): Either[ErrorMessage, R]
  def map[S](f: R => S): Readable[S] = (s: String) => read(s).map(f)
  def flatMap[S](f: R => Either[String, S]) = (s: String) => read(s).flatMap(f)
}

object Readable {
  implicit val string: Readable[String] = (s: String) => Right(s)
  implicit val email: Readable[Email] = string.map(Email.apply)
}

case class ParsedJWT(
  jwt: SignedJWT,
  claims: JWTClaimsSet,
  kid: String,
  iss: Issuer,
  exp: Instant,
  token: TokenValue
) {

  import scala.collection.JavaConverters.asScalaBufferConverter

  def parse[T](key: String)(implicit r: Readable[T]): Either[JWTError, T] =
    readString(key).flatMap { s => r.read(s).left.map(err => InvalidClaims(token, err)) }

  def readString(key: String): Either[JWTError, String] =
    read(claims.getStringClaim(key), key)

  def readStringOpt(key: String) = read(Option(claims.getStringClaim(key)), key)

  def readStringListOrEmpty(key: String): Either[JWTError, Seq[String]] =
    readStringList(key).map(_.getOrElse(Nil))

  def readStringList(key: String): Either[JWTError, Option[Seq[String]]] =
    read(Option(claims.getStringListClaim(key)).map(_.asScala.toList), key)

  def readBoolean(key: String): Either[JWTError, Boolean] =
    read(claims.getBooleanClaim(key), key)

  def read[T](danger: => T, key: String): Either[JWTError, T] =
    StaticTokenValidator.read(token, danger, ErrorMessage(s"Claim missing: '$key'."))
}

case class Verified(parsed: ParsedJWT) {
  def expiresIn: Duration = (parsed.exp.toEpochMilli - Instant.now().toEpochMilli).millis
  def readString(key: String) = parsed.readString(key)
  def readBoolean(key: String) = parsed.readBoolean(key)
  def token = parsed.token
}

case class KeyConf(
  n: Base64URL,
  kid: String,
  use: KeyUse,
  e: Base64URL,
  alg: JWSAlgorithm,
  kty: String
)

object KeyConf {
  implicit val reader = Reads[KeyConf] { json =>
    for {
      n <- (json \ "n").validate[String].map(new Base64URL(_))
      kid <- (json \ "kid").validate[String]
      use <- (json \ "use").validate[String].flatMap(parseUse)
      e <- (json \ "e").validate[String].map(new Base64URL(_))
      kty <- (json \ "kty").validate[String]
    } yield KeyConf(n, kid, use, e, JWSAlgorithm.RS256, kty)
  }

  def parseUse(s: String): JsResult[KeyUse] =
    try {
      JsSuccess(KeyUse.parse(s))
    } catch {
      case pe: ParseException => JsError(pe.getMessage)
    }

  def rsa(n: String, kid: String, issuer: String) = KeyConf(
    new Base64URL(n),
    kid,
    KeyUse.SIGNATURE,
    new Base64URL("AQAB"),
    JWSAlgorithm.RS256,
    "RSA"
  )
}

case class JWTKeys(keys: Seq[KeyConf])

object JWTKeys {
  implicit val json = Json.reads[JWTKeys]
}

trait JWTUser {
  def username: Username
}

case class CognitoUser(
  username: Username,
  email: Option[Email],
  groups: Seq[String],
  verified: Verified
) extends JWTUser
