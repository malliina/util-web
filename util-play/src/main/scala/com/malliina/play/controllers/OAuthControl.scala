package com.malliina.play.controllers

import java.math.BigInteger
import java.security.SecureRandom

import akka.stream.Materializer
import com.malliina.oauth.GoogleOAuth.{CODE, STATE}
import com.malliina.oauth.{GoogleOAuth, GoogleOAuthReader}
import com.malliina.play.controllers.OAuthControl.log
import com.malliina.play.json.JsonMessages
import play.api.Logger
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** A [[Controller]] to handle the Google OAuth2 authentication flow.
  *
  * 1) User is sent to initiate()
  * 2) initiate() sends (redirects) user to Google
  * 3) Google redirects user to redirResponse() after consent
  * 4) redirResponse() extracts email, authenticates
  */
trait OAuthControl extends Controller {
  val messageKey = "message"
  val logoutMessage = "You have successfully signed out."
  val creds = GoogleOAuthReader.load
  val oauth = new GoogleOAuth(creds.clientId, creds.clientSecret)

  implicit def mat: Materializer

  def isAuthorized(email: String): Boolean

  def startOAuth: Call

  def oAuthRedir: Call

  def onOAuthSuccess: Call

  def ejectCall: Call

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
              log info s"User: $email logged in."
              Redirect(onOAuthSuccess).withSession(sessionUserKey -> email)
            } else {
              log warn s"User: $email authenticated successfully but is not authorized."
              onOAuthUnauthorized(email)
            }
          })
        })
      } else {
        val msg = "Invalid state parameter in OAuth callback."
        log warn msg
        fut(fail(msg))
      }
    })
  })

  def sessionUserKey = Security.username

  //  protected override def onUnauthorized(implicit headers: RequestHeader): Result = Redirect(initiateOAuth)

  def onOAuthUnauthorized(email: String) = ejectWith(unauthorizedMessage(email)) //fail(unauthorizedMessage(email))

  protected def ejectWith(message: String) = Redirect(ejectCall).flashing(messageKey -> message)

  def unauthorizedMessage(email: String) = s"Hi $email, you're not authorized."

  def noConsentFailure = {
    val msg = "The user did not consent to the OAuth request."
    log info msg
    fail(msg)
  }

  def fail(msg: String) = Unauthorized(JsonMessages failure msg)

  def fut[T](item: T) = Future successful item
}

object OAuthControl {
  private val log = Logger(getClass)
}
