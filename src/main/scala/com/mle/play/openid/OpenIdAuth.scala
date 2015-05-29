package com.mle.play.openid

import com.mle.util.Log
import play.api.Play.current
import play.api.libs.openid.{OpenID, UserInfo}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * OpenID authentication. Only Google is supported for now.
 * Usage:
 * <p>
 * Set a route in your routes file that leads to the <code>handleOpenId</code> method.
 * For example:
 * {{{GET  /oid  controllers.App.handleOpenId}}}
 *
 * Set another route that leads to the <code>openIdCallback</code> method, for example:
 * {{{GET  /openidcb  controllers.App.openIdCallback}}}
 *
 * Define the openIdCallbackRoute of this trait to the route you specified:
 * {{{protected def openIdCallbackRoute = routes.App.openIdCallback()}}}
 *
 * To start authentication, send the user to the OpenID route you first defined:
 * {{{Redirect(routes.App.handleOpenId())}}}
 *
 * The authenticated email address is stored in the username session variable.
 *
 * @author Michael
 */
trait OpenIdAuth extends Log {
  /**
   * @return routes.YourPlayController.openIdCallback
   */
  protected def openIdCallbackRoute: Call

  /**
   * Implement something like: <code>routes.Controller.homePage()</code>
   *
   * @return the place to go after the user is successfully authenticated
   */
  def successRedirect: Call

  /**
   * Suggestion: redirect to an error page with an error message.
   */
  def onOpenIdFailure: Result

  /**
   *
   * @param email email to validate
   * @return true if the email is authorized to sign in, false otherwise
   */
  def isAuthorized(email: String): Boolean

  /**
   * Called if the user has successfully authenticated, but is not
   * authorized to access the resource/site.
   *
   * @param userInfo authenticated user
   * @return the result
   */
  def onOpenIdUnauthorized(userInfo: UserInfo): Result = {
    val user = userString(userInfo)
    Unauthorized(s"Hi $user, you're not authorized.")
  }

  def userString(user: UserInfo): String = {
    val firstName = user attributes "first"
    val email = user attributes "email"
    s"$firstName ($email)"
  }

  def handleOpenId = Action.async(implicit request => {
    OpenID.redirectURL(
      "https://www.google.com/accounts/o8/id",
      callbackURL = openIdCallbackRoute.absoluteURL(),
      axRequired = Seq(
        "email" -> "http://axschema.org/contact/email",
        "first" -> "http://axschema.org/namePerson/first"
      )).map(url => Redirect(url)).recover {
      case ex => handleOpenIdFailure(ex, request)
    }
  })

  def openIdCallback = Action.async(implicit request => {
    OpenID.verifiedId.map(userInfo => {
      val email = userInfo attributes "email"
      val user = userString(userInfo)
      if (isAuthorized(email)) {
        log info s"User $user logged in"
        Redirect(successRedirect).withSession(Security.username -> email)
      } else {
        log warn s"User $user authenticated successfully but is not authorized"
        onOpenIdUnauthorized(userInfo)
      }
    }).recover {
      case ex => handleOpenIdFailure(ex, request)
    }
  })

  /**
   * Called if the OpenID authentication process fails.
   *
   * Note that this is not called if the authorization fails;
   * see <code>onOpenIdUnauthorized(UserInfo)</code> in that case.
   */
  protected def handleOpenIdFailure(ex: Throwable, request: RequestHeader): Result = {
    val addr = request.remoteAddress
    val errorMsg = Option(ex).map(ex => s": ${ex.getMessage}").getOrElse("")
    log warn s"OpenID authentication failed from: $addr with error: $errorMsg"
    onOpenIdFailure
  }
}
