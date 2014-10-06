package com.mle.oauth

import java.math.BigInteger
import java.security.SecureRandom

import com.mle.oauth.GoogleOAuth.{CODE, STATE}
import com.mle.play.json.JsonMessages
import com.mle.util.Log
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * A [[Controller]] to handle the Google OAuth2 authentication flow.
 *
 * 1) User is sent to initiate()
 * 2) initiate() sends (redirects) user to Google
 * 3) Google redirects user to redirResponse() after consent
 * 4) redirResponse() extracts email, authenticates
 *
 * @author Michael
 */
trait OAuthControl extends Controller with Log {
  val creds = GoogleOAuthReader.load
  val oauth = new GoogleOAuth(creds.clientId, creds.clientSecret)

  def isAuthorized(email: String): Boolean

  def oAuthRedir: Call

  def onOAuthSuccess: Call

  def redirURL(implicit req: RequestHeader) = oAuthRedir.absoluteURL(req.secure)

  def discover() = oauth.discover()

  def initiate = Action.async(implicit request => {
    discover().map(conf => {
      val state = new BigInteger(130, new SecureRandom()).toString(32)
      log debug s"Redirecting user to Google OAuth..."
      Redirect(oauth.authRequestUri(conf.authorizationEndpoint, redirURL, state))
        .withSession(STATE -> state)
    })
  })

  def redirResponse = Action.async(implicit request => {
    request.getQueryString(CODE).fold(fut(noConsentFailure))(code => {
      val requestState = request.getQueryString(STATE)
      val sessionState = request.session.get(STATE)
      val isStateOk = requestState.exists(rs => sessionState.contains(rs))
      if (isStateOk) {
        discover().flatMap(conf => {
          // exchanges code for token, which contains the user's email address
          oauth.resolveEmail(conf.tokenEndpoint, code, redirURL).map(email => {
            if (isAuthorized(email)) {
              log info s"User $email logged in."
              Redirect(onOAuthSuccess).withSession(Security.username -> email)
            } else {
              log warn s"User $email authenticated successfully but is not authorized."
              onOAuthUnauthorized(email)
            }
          })
        })
      } else {
        log warn s"Invalid state parameter in OAuth callback."
        fut(fail("Invalid state parameter."))
      }
    })
  })

  def onOAuthUnauthorized(email: String) = fail(unauthorizedMessage(email))

  def noConsentFailure = {
    log info "The user did not consent to the OAuth request."
    fail("The user did not consent.")
  }

  def unauthorizedMessage(email: String) = s"Hi $email, you're not authorized."

  def fail(msg: String) = Unauthorized(JsonMessages failure msg)

  def fut[T](item: T) = Future successful item
}
