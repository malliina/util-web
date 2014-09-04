package com.mle.play.controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Controller, Request}

/**
 *
 * @author mle
 */
trait AccountController extends Controller with BaseSecurity {

  val loginForm = Form(tuple(
    "username" -> nonEmptyText,
    "password" -> nonEmptyText
  ) verifying("Invalid credentials.", _ match {
    case (username, password) => validateCredentials(username, password)
  }))

  def changePasswordForm(implicit request: Request[AnyContent]) = Form(tuple(
    "oldPassword" -> nonEmptyText.verifying("Incorrect old password.", validateCredentials(authenticate(request).get, _)),
    "newPassword" -> nonEmptyText,
    "newPasswordAgain" -> nonEmptyText
  ).verifying("The new password was incorrectly repeated.", in => in match {
    case (_, newPass, newPassAgain) => newPass == newPassAgain
  }))
}
