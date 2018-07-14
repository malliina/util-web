package com.malliina.play.controllers

import com.malliina.play.auth.{BasicCredentials, RememberMeCredentials}
import com.malliina.play.forms.FormMappings.{password, username}
import com.malliina.play.models.PasswordChange
import play.api.data.Form
import play.api.data.Forms._

object AccountForms extends AccountForms

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
    userFormKey -> username,
    passFormKey -> password
  )(BasicCredentials.apply)(BasicCredentials.unapply))

  val rememberMeLoginForm = Form(mapping(
    userFormKey -> username,
    passFormKey -> password,
    rememberMeKey -> boolean // the checkbox HTML element must have the property 'value="true"'
  )(RememberMeCredentials.apply)(RememberMeCredentials.unapply))

  val changePasswordForm = Form(mapping(
    oldPassKey -> password,
    newPassKey -> password,
    newPassAgainKey -> password
  )(PasswordChange.apply)(PasswordChange.unapply)
    .verifying("The new password was incorrectly repeated.", pc => pc.newPass == pc.newPassAgain))
}
