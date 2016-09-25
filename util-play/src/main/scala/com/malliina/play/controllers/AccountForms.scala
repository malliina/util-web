package com.malliina.play.controllers

import com.malliina.play.auth.{BasicCredentials, RememberMeCredentials}
import com.malliina.play.models.{Password, PasswordChange, Username}
import play.api.data.Form
import play.api.data.Forms._

class AccountForms {
  val intendedUri = "intended_uri"
  val feedback = "feedback"
  val userFormKey = "username"
  val passFormKey = "password"
  val rememberMeKey = "remember"

  val oldPassKey = "oldPassword"
  val newPassKey = "newPassword"
  val newPassAgainKey = "newPasswordAgain"

  val loginForm = Form[BasicCredentials](mapping(
    userFormKey -> Username.mapping,
    passFormKey -> Password.mapping
  )(BasicCredentials.apply)(BasicCredentials.unapply))

  val rememberMeLoginForm = Form(mapping(
    userFormKey -> Username.mapping,
    passFormKey -> Password.mapping,
    rememberMeKey -> boolean // the checkbox HTML element must have the property 'value="true"'
  )(RememberMeCredentials.apply)(RememberMeCredentials.unapply))

  val changePasswordForm = Form(mapping(
    oldPassKey -> Password.mapping,
    newPassKey -> Password.mapping,
    newPassAgainKey -> Password.mapping
  )(PasswordChange.apply)(PasswordChange.unapply)
    .verifying("The new password was incorrectly repeated.", pc => pc.newPass == pc.newPassAgain))
}
