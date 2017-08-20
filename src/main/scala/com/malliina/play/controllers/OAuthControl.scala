package com.malliina.play.controllers

import java.math.BigInteger
import java.security.SecureRandom

import akka.stream.Materializer
import com.malliina.http.FullUrl
import com.malliina.oauth.GoogleOAuth.{Code, State}
import com.malliina.oauth.{GoogleOAuth, GoogleOAuthCredentials, GoogleOAuthLike, GoogleOAuthReader}
import com.malliina.play.controllers.OAuthControl.log
import com.malliina.play.http.FullUrls
import com.malliina.play.json.JsonMessages
import com.malliina.play.models.Email
import play.api.Logger
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/** A template to handle the Google OAuth2 authentication flow.
  *
  * 1) User is sent to initiate()
  * 2) initiate() sends (redirects) user to Google
  * 3) Google redirects user to redirResponse() after consent
  * 4) redirResponse() extracts email, authenticates
  */
abstract class OAuthControl(val actions: ActionBuilder[Request, AnyContent],
                            val oauth: GoogleOAuthLike,
                            val ec: ExecutionContext)
  extends AutoCloseable {
  def this(actions: ActionBuilder[Request, AnyContent], creds: GoogleOAuthCredentials, mat: Materializer) =
    this(actions, new GoogleOAuth(creds.clientId, creds.clientSecret, mat), mat.executionContext)
  def this(actions: ActionBuilder[Request, AnyContent], mat: Materializer) =
    this(actions, GoogleOAuthReader.load, mat)


  implicit val exec = ec
  val messageKey = "message"
  val logoutMessage = "You have successfully signed out."

  def sessionUserKey = "username"

  def isAuthorized(email: Email): Boolean

  def startOAuth: Call

  def oAuthRedir: Call

  def onOAuthSuccess: Call

  def ejectCall: Call

  def redirURL(request: RequestHeader): FullUrl =
    FullUrls(oAuthRedir, request)

  def discover() = oauth.discover()

  def initiate = actions.async { request =>
    discover() map { conf =>
      // TODO document
      val state = new BigInteger(130, new SecureRandom()).toString(32)
      log debug s"Redirecting user to Google OAuth..."
      Results.Redirect(oauth.authRequestUri(conf.authorizationEndpoint, redirURL(request), state).url)
        .withSession(State -> state)
    }
  }

  def redirResponse = actions.async { request =>
    request.getQueryString(Code).fold(fut(noConsentFailure)) { code =>
      val requestState = request.getQueryString(State)
      val sessionState = request.session.get(State)
      val isStateOk = requestState.exists(rs => sessionState.contains(rs))
      if (isStateOk) {
        discover() flatMap { conf =>
          // exchanges code for token, which contains the user's email address
          oauth.resolveEmail(conf.tokenEndpoint, code, redirURL(request)) map { email =>
            if (isAuthorized(email)) {
              log info s"User '$email' logged in."
              Redirect(onOAuthSuccess).withSession(sessionUserKey -> email.email)
            } else {
              log warn s"User '$email' authenticated successfully but is not authorized."
              onOAuthUnauthorized(email)
            }
          }
        }
      } else {
        val msg = "Invalid state parameter in OAuth callback."
        log warn msg
        fut(fail(msg))
      }
    }
  }

  def onOAuthUnauthorized(email: Email) = ejectWith(unauthorizedMessage(email))

  def eject: Result = ejectWith(logoutMessage)

  def ejectWith(message: String) = Redirect(ejectCall).flashing(messageKey -> message)

  def unauthorizedMessage(email: Email) = s"Hi $email, you're not authorized."

  def noConsentFailure = {
    val msg = "The user did not consent to the OAuth request."
    log info msg
    fail(msg)
  }

  def fail(msg: String) = Unauthorized(JsonMessages failure msg)

  def fut[T](item: T) = Future successful item

  override def close(): Unit = oauth.close()
}

object OAuthControl {
  private val log = Logger(getClass)
}
