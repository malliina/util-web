package com.mle.play.controllers

import com.mle.play.auth.BasicCredentials
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Controller, Request, RequestHeader}

/**
 *
 * @author mle
 */
trait AccountController extends Controller with BaseSecurity {
  val INTENDED_URI = "intended_uri"
  val FEEDBACK = "feedback"
  val userFormKey = "username"
  val passFormKey = "password"
  val rememberMeKey = "remember"

  val oldPassKey = "oldPassword"
  val newPassKey = "newPassword"
  val newPassAgainKey = "newPasswordAgain"

  val loginForm = Form[BasicCredentials](mapping(
    userFormKey -> nonEmptyText,
    passFormKey -> nonEmptyText
  )(BasicCredentials.apply)(BasicCredentials.unapply)
    .verifying("Invalid credentials.", creds => validateCredentials(creds)))

  def changePasswordForm(implicit request: Request[AnyContent]) = Form(tuple(
    oldPassKey -> nonEmptyText.verifying("Incorrect old password.", pass => validateCredentials(BasicCredentials(authenticate(request).get.user, pass))),
    newPassKey -> nonEmptyText,
    newPassAgainKey -> nonEmptyText
  ).verifying("The new password was incorrectly repeated.", in => in match {
    case (_, newPass, newPassAgain) => newPass == newPassAgain
  }))

  protected def logUnauthorized(implicit request: RequestHeader) {
    log warn s"Unauthorized request: ${request.path} from: ${request.remoteAddress}"
  }
}
