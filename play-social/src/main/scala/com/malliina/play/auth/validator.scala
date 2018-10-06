package com.malliina.play.auth

import java.text.ParseException
import java.time.Instant

import com.malliina.play.auth.StaticTokenValidator.read
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import play.api.Logger

case class Code(code: String)

object StaticTokenValidator {
  private val log = Logger(getClass)

  def read[T](token: TokenValue, f: => T, onMissing: => String): Either[JWTError, T] =
    try {
      Option(f).toRight(MissingData(token, onMissing))
    } catch {
      case pe: ParseException =>
        log.error(s"Parse error for token '$token'.", pe)
        Left(ParseError(token, pe))
    }
}

/**
  * @param keys   public keys used to validate tokens
  * @param issuer issuer
  * @tparam T type of token
  * @tparam U type of user
  */
abstract class StaticTokenValidator[T <: TokenValue, U](keys: Seq[KeyConf], issuer: String)
  extends TokenValidator(issuer) {

  def validate(token: T): Either[JWTError, U] =
    super.validate(token, keys, Instant.now()).flatMap(toUser)

  protected def toUser(v: Verified): Either[JWTError, U]
}

object GoogleValidator {
  val issuers = Seq("https://accounts.google.com", "accounts.google.com")

  def apply(clientId: String) = new GoogleValidator(clientId, issuers)
}

class GoogleValidator(clientId: String, issuers: Seq[String]) extends TokenValidator(issuers) {
  override protected def validateClaims(parsed: ParsedJWT, now: Instant): Either[JWTError, ParsedJWT] = {
    for {
      _ <- checkContains(CognitoValidator.Aud, clientId, parsed)
    } yield parsed
  }
}

object MicrosoftValidator {
  val issuerMicrosoftConsumer = "https://login.microsoftonline.com/9188040d-6c67-4c5b-b112-36a304b66dad/v2.0"

  def apply(clientId: String) = new MicrosoftValidator(clientId, issuerMicrosoftConsumer)
}

class MicrosoftValidator(clientId: String, issuer: String) extends TokenValidator(issuer) {
  override protected def validateClaims(parsed: ParsedJWT, now: Instant): Either[JWTError, ParsedJWT] =
    for {
      _ <- checkContains(CognitoValidator.Aud, clientId, parsed)
      _ <- checkNbf(parsed, now)
    } yield parsed

  def checkNbf(parsed: ParsedJWT, now: Instant): Either[JWTError, Instant] =
    read(parsed.token, parsed.claims.getNotBeforeTime, "nbf").flatMap { nbf =>
      val nbfInstant = nbf.toInstant
      if (now.isBefore(nbfInstant)) Left(NotYetValid(parsed.token, nbfInstant, now))
      else Right(nbfInstant)
    }
}

abstract class TokenValidator(issuers: Seq[String]) {
  def this(issuer: String) = this(Seq(issuer))

  protected def validateClaims(parsed: ParsedJWT, now: Instant): Either[JWTError, ParsedJWT]

  def validate(token: TokenValue, keys: Seq[KeyConf], now: Instant): Either[JWTError, Verified] =
    for {
      parsed <- parse(token)
      verified <- verify(parsed, keys, now)
    } yield verified

  protected def parse(token: TokenValue): Either[JWTError, ParsedJWT] = for {
    jwt <- read(token, SignedJWT.parse(token.token), "token")
    claims <- read(token, jwt.getJWTClaimsSet, "claims")
    kid <- read(token, jwt.getHeader.getKeyID, "kid")
    iss <- read(token, claims.getIssuer, "iss")
    exp <- read(token, claims.getExpirationTime, "exp")
  } yield ParsedJWT(jwt, claims, kid, iss, exp.toInstant, token)

  protected def verify(parsed: ParsedJWT, keys: Seq[KeyConf], now: Instant): Either[JWTError, Verified] = {
    val now = Instant.now()
    val token = parsed.token
    if (!issuers.contains(parsed.iss)) {
      Left(IssuerMismatch(token, parsed.iss, issuers))
    } else {
      keys.find(_.kid == parsed.kid).map { keyConf =>
        val verifier = buildVerifier(keyConf)
        if (!isSignatureValid(parsed.jwt, verifier)) Left(InvalidSignature(token))
        else if (!now.isBefore(parsed.exp)) Left(Expired(token, parsed.exp, now))
        else validateClaims(parsed, now).map(p => Verified(p))
      }.getOrElse {
        Left(InvalidKeyId(token, parsed.kid, keys.map(_.kid)))
      }
    }
  }

  protected def isSignatureValid(unverified: SignedJWT, verifier: RSASSAVerifier): Boolean =
    unverified.verify(verifier)

  def checkClaim(key: String, expected: String, parsed: ParsedJWT): Either[JWTError, ParsedJWT] = {
    parsed.readString(key).flatMap { actual =>
      if (actual == expected) Right(parsed)
      else Left(InvalidClaims(parsed.token, s"Claim '$key' must equal '$expected', was '$actual'."))
    }
  }

  def checkContains(key: String, expected: String, parsed: ParsedJWT): Either[JWTError, Seq[String]] = {
    parsed.readStringListOrEmpty(key).flatMap { arr =>
      if (arr.contains(expected)) Right(arr)
      else Left(InvalidClaims(parsed.token, s"Claim '$key' does not contain '$expected', was '${arr.mkString(", ")}'."))
    }
  }

  def buildVerifier(conf: KeyConf) = {
    val rsaKey = new RSAKey.Builder(conf.n, conf.e)
      .keyUse(conf.use)
      .keyID(conf.kid)
      .build()
    new RSASSAVerifier(rsaKey)
  }
}

class LiberalValidator(conf: KeyConf, issuer: String)
  extends StaticTokenValidator[AccessToken, Verified](Seq(conf), issuer) {
  override protected def validateClaims(parsed: ParsedJWT, now: Instant): Either[JWTError, ParsedJWT] =
    Right(parsed)

  override protected def toUser(v: Verified) =
    Right(v)
}
