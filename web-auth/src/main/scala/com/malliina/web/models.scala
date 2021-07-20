package com.malliina.web

import java.text.ParseException
import java.time.Instant

import com.malliina.http.FullUrl
import com.malliina.json.PrimitiveFormats.durationCodec
import com.malliina.values._
import com.malliina.web.OAuthKeys.{ClientIdKey, ClientSecretKey, CodeKey, RedirectUri, Scope}
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import io.circe._
import io.circe.generic.semiauto._

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

case class Start(
  authorizationEndpoint: FullUrl,
  params: Map[String, String],
  nonce: Option[String]
)

case class Callback(
  requestState: Option[String],
  sessionState: Option[String],
  codeQuery: Option[String],
  requestNonce: Option[String],
  redirectUrl: FullUrl
)

object RequestToken {
  val Key = "request_token"
}

case class CognitoTokensJson(
  access_token: AccessToken,
  id_token: IdToken,
  refresh_token: RefreshToken
) {
  def canonical = CognitoTokens(access_token, id_token, refresh_token)
}

object CognitoTokensJson {
  implicit val json: Codec[CognitoTokensJson] = deriveCodec[CognitoTokensJson]
}

case class CognitoTokens(accessToken: AccessToken, idToken: IdToken, refreshToken: RefreshToken) {
  def json = CognitoTokensJson(accessToken, idToken, refreshToken)
}

object CognitoTokens {
  implicit val json: Codec[CognitoTokens] = Codec.from(
    CognitoTokensJson.json.map(_.canonical),
    deriveEncoder[CognitoTokens]
  )
}

case class GitHubTokensJson(access_token: AccessToken, token_type: Option[String]) {
  def canonical = GitHubTokens(access_token, token_type)
}

object GitHubTokensJson {
  implicit val json: Codec[GitHubTokensJson] = deriveCodec[GitHubTokensJson]
}

case class GitHubTokens(accessToken: AccessToken, tokenType: Option[String]) {
  def json = GitHubTokensJson(accessToken, tokenType)
}

object GitHubTokens {
  implicit val json: Codec[GitHubTokens] = Codec.from(
    GitHubTokensJson.json.map(_.canonical),
    deriveEncoder[GitHubTokens]
  )
}

case class GitHubEmail(
  email: Email,
  primary: Boolean,
  verified: Boolean,
  visibility: Option[String]
)

object GitHubEmail {
  implicit val json: Codec[GitHubEmail] = deriveCodec[GitHubEmail]
}

trait OpenIdConf {
  def jwksUri: FullUrl
}

case class SimpleOpenIdConf(jwksUri: FullUrl) extends OpenIdConf

object SimpleOpenIdConf {
  implicit val decoder: Decoder[SimpleOpenIdConf] =
    Decoder.forProduct1("jwks_uri")(apply)
}

case class AuthEndpoints(authorizationEndpoint: FullUrl, tokenEndpoint: FullUrl, jwksUri: FullUrl)
  extends OpenIdConf

object AuthEndpoints {
  implicit val decoder: Decoder[AuthEndpoints] =
    Decoder.forProduct3("authorization_endpoint", "token_endpoint", "jwks_uri")(apply)
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
  implicit val decoder: Decoder[MicrosoftOAuthConf] =
    Decoder.forProduct7(
      "authorization_endpoint",
      "token_endpoint",
      "jwks_uri",
      "end_session_endpoint",
      "scopes_supported",
      "issuer",
      "claims_supported"
    )(apply)
}

trait TokenSet {
  def idToken: IdToken
}

case class SimpleTokens(idToken: IdToken)

object SimpleTokens {
  implicit val decoder: Decoder[SimpleTokens] =
    Decoder.forProduct1("id_token")(apply)
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
  implicit val decoder: Decoder[MicrosoftTokens] =
    Decoder.forProduct6(
      "id_token",
      "access_token",
      "refresh_token",
      "token_type",
      "expires_in",
      "scope"
    )(apply)
}

case class GoogleTokens(
  idToken: IdToken,
  accessToken: AccessToken,
  expiresIn: Duration,
  tokenType: String
) extends TokenSet

object GoogleTokens {
  implicit val decoder: Decoder[GoogleTokens] =
    Decoder.forProduct4(
      "id_token",
      "access_token",
      "expires_in",
      "token_type"
    )(apply)
}

case class FacebookTokens(accessToken: AccessToken, tokenType: String, expiresIn: Duration)

object FacebookTokens {
  implicit val decoder: Decoder[FacebookTokens] =
    Decoder.forProduct3(
      "access_token",
      "token_type",
      "expires_in"
    )(apply)
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
  implicit val decoder: Decoder[TwitterUser] =
    Decoder.forProduct4(
      "id_str",
      "name",
      "screen_name",
      "email"
    )(apply)
}

case class EmailResponse(email: Email)

object EmailResponse {
  implicit val json: Codec[EmailResponse] = deriveCodec[EmailResponse]
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

  import scala.jdk.CollectionConverters.CollectionHasAsScala

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
  implicit val keyUseDecoder: Decoder[KeyUse] = Decoder.decodeString.emap(s => parseUse(s))
  implicit val base64UrlDecoder: Decoder[Base64URL] =
    Decoder.decodeString.map(s => new Base64URL(s))
  implicit val decoder: Decoder[KeyConf] =
    Decoder.forProduct5(
      "n",
      "kid",
      "use",
      "e",
      "kty"
    )(rs256)

  def rs256(n: Base64URL, kid: String, use: KeyUse, e: Base64URL, kty: String) =
    KeyConf(n, kid, use, e, JWSAlgorithm.RS256, kty)

  def parseUse(s: String): Either[String, KeyUse] =
    try {
      Right(KeyUse.parse(s))
    } catch {
      case pe: ParseException => Left(pe.getMessage)
    }

  def rsa(n: String, kid: String) = KeyConf(
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
  implicit val json: Decoder[JWTKeys] = deriveDecoder[JWTKeys]
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

sealed abstract class IdentityProvider(val name: String)

object IdentityProvider {
  case object LoginWithAmazon extends IdentityProvider("LoginWithAmazon")
  case object IdentityFacebook extends IdentityProvider("Facebook")
  case object IdentityGoogle extends IdentityProvider("Google")
  case class IdentityOther(n: String) extends IdentityProvider(n)
}

case class AuthCodeConf(
  brandName: String,
  conf: AuthConf,
  client: KeyClient,
  extraStartParams: Map[String, String] = Map.empty,
  extraValidateParams: Map[String, String] = Map.empty
)

case class OAuthParams(
  client: KeyClient,
  conf: AuthConf,
  extraStartParams: Map[String, String] = Map.empty,
  extraValidateParams: Map[String, String] = Map.empty
) {
  protected def commonAuthParams(authScope: String, redirectUrl: FullUrl): Map[String, String] =
    Map(
      RedirectUri -> redirectUrl.url,
      ClientIdKey -> conf.clientId.value,
      Scope -> authScope
    )

  /** Not encoded.
    */
  protected def validationParams(code: Code, redirectUrl: FullUrl): Map[String, String] = Map(
    ClientIdKey -> conf.clientId.value,
    ClientSecretKey -> conf.clientSecret.value,
    RedirectUri -> redirectUrl.url,
    CodeKey -> code.code
  )
}

case class StaticConf(
  scope: String,
  authorizationEndpoint: FullUrl,
  tokenEndpoint: FullUrl,
  authConf: AuthConf
) {
  def clientId = authConf.clientId
  def clientSecret = authConf.clientSecret
}
