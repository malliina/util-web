package com.malliina.play.auth

import java.text.ParseException
import java.time.Instant

import com.malliina.play.auth.StaticTokenValidator.read
import com.malliina.values.ErrorMessage
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import play.api.Logger

object StaticTokenValidator {
  private val log = Logger(getClass)

  def read[T](token: TokenValue, f: => T, onMissing: => ErrorMessage): Either[JWTError, T] =
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

abstract class TokenValidator(issuers: Seq[String]) extends ClaimKeys {
  def this(issuer: String) = this(Seq(issuer))

  protected def validateClaims(parsed: ParsedJWT, now: Instant): Either[JWTError, ParsedJWT]

  def validate(token: TokenValue, keys: Seq[KeyConf], now: Instant): Either[JWTError, Verified] =
    for {
      parsed <- parse(token)
      verified <- verify(parsed, keys, now)
    } yield verified

  protected def parse(token: TokenValue): Either[JWTError, ParsedJWT] = for {
    jwt <- read(token, SignedJWT.parse(token.token), ErrorMessage("token"))
    claims <- read(token, jwt.getJWTClaimsSet, ErrorMessage("claims"))
    kid <- read(token, jwt.getHeader.getKeyID, ErrorMessage(Kid))
    iss <- read(token, claims.getIssuer, ErrorMessage(IssuerKey))
    exp <- read(token, claims.getExpirationTime, ErrorMessage(Exp))
  } yield ParsedJWT(jwt, claims, kid, iss, exp.toInstant, token)

  protected def verify(
    parsed: ParsedJWT,
    keys: Seq[KeyConf],
    now: Instant
  ): Either[JWTError, Verified] = {
    val now = Instant.now()
    val token = parsed.token
    if (!issuers.contains(parsed.iss)) {
      Left(IssuerMismatch(token, parsed.iss, issuers))
    } else {
      keys
        .find(_.kid == parsed.kid)
        .map { keyConf =>
          val verifier = buildVerifier(keyConf)
          if (!isSignatureValid(parsed.jwt, verifier)) Left(InvalidSignature(token))
          else if (!now.isBefore(parsed.exp)) Left(Expired(token, parsed.exp, now))
          else validateClaims(parsed, now).map(p => Verified(p))
        }
        .getOrElse {
          Left(InvalidKeyId(token, parsed.kid, keys.map(_.kid)))
        }
    }
  }

  protected def isSignatureValid(unverified: SignedJWT, verifier: RSASSAVerifier): Boolean =
    unverified.verify(verifier)

  def checkClaim(key: String, expected: String, parsed: ParsedJWT): Either[JWTError, ParsedJWT] = {
    parsed.readString(key).flatMap { actual =>
      if (actual == expected) Right(parsed)
      else
        Left(
          InvalidClaims(
            parsed.token,
            ErrorMessage(s"Claim '$key' must equal '$expected', was '$actual'.")
          )
        )
    }
  }

  def checkContains(
    key: String,
    expecteds: Seq[String],
    parsed: ParsedJWT
  ): Either[JWTError, Seq[String]] = {
    parsed.readStringListOrEmpty(key).flatMap { arr =>
      if (expecteds.exists(e => arr.contains(e))) Right(arr)
      else
        Left(
          InvalidClaims(
            parsed.token,
            ErrorMessage(
              s"Claim '$key' does not contain any of '${expecteds.mkString(", ")}', was '${arr.mkString(", ")}'."
            )
          )
        )
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

/** Accepts any claims, provides user as-is. */
class LiberalValidator(conf: KeyConf, issuer: String)
  extends StaticTokenValidator[AccessToken, Verified](Seq(conf), issuer) {
  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    Right(parsed)

  override protected def toUser(v: Verified) =
    Right(v)
}
