package com.malliina.play.controllers

import akka.stream.Materializer
import com.malliina.play.auth.BasicCredentials
import com.malliina.play.controllers.AccountController.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

abstract class AccountController(auth: BaseSecurity) {
  def this(mat: Materializer) = this(new BaseSecurity(Security.username, mat))

  val intendedUri = "intended_uri"
  val feedback = "feedback"
  val userFormKey = "username"
  val passFormKey = "password"
  val rememberMeKey = "remember"

  val oldPassKey = "oldPassword"
  val newPassKey = "newPassword"
  val newPassAgainKey = "newPasswordAgain"

  val loginForm = Form[BasicCredentials](mapping(
    userFormKey -> nonEmptyText,
    passFormKey -> nonEmptyText
  )(BasicCredentials.apply)(BasicCredentials.unapply))

  def changePasswordForm(request: Request[AnyContent]) = Form(tuple(
    oldPassKey -> nonEmptyText,
    newPassKey -> nonEmptyText,
    newPassAgainKey -> nonEmptyText
  ).verifying("The new password was incorrectly repeated.", in => in match {
    case (_, newPass, newPassAgain) => newPass == newPassAgain
  }))

  protected def logUnauthorized(request: RequestHeader): Unit =
    log warn s"Unauthorized request: ${request.path} from: ${request.remoteAddress}"
}

object AccountController {
  private val log = Logger(getClass)
}
