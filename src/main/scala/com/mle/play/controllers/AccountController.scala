package com.mle.play.controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Controller, Request}

/**
 *
 * @author mle
 */
trait AccountController extends Controller with BaseSecurity {

  val userFormKey = "username"
  val passFormKey = "password"
  val rememberMeKey = "remember"

  val oldPassKey = "oldPassword"
  val newPassKey = "newPassword"
  val newPassAgainKey = "newPasswordAgain"

  val loginForm = Form(tuple(
    userFormKey -> nonEmptyText,
    passFormKey -> nonEmptyText
  ) verifying("Invalid credentials.", _ match {
    case (username, password) => validateCredentials(username, password)
  }))

  def changePasswordForm(implicit request: Request[AnyContent]) = Form(tuple(
    oldPassKey -> nonEmptyText.verifying("Incorrect old password.", validateCredentials(authenticate(request).get.user, _)),
    newPassKey -> nonEmptyText,
    newPassAgainKey -> nonEmptyText
  ).verifying("The new password was incorrectly repeated.", in => in match {
    case (_, newPass, newPassAgain) => newPass == newPassAgain
  }))
}
