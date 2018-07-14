package com.malliina.play.forms

import com.malliina.values._
import play.api.data.format.Formats.{longFormat, stringFormat}
import play.api.data.{Forms, Mapping}

object FormMappings extends FormMappings

trait FormMappings {
  val username: Mapping[Username] = stringMapping[Username](Username.apply)
  val password: Mapping[Password] = stringMapping[Password](Password.apply)
  val email: Mapping[Email] = stringMapping[Email](Email.apply)
  val accessToken: Mapping[AccessToken] = stringMapping[AccessToken](AccessToken.apply)
  val idToken: Mapping[IdToken] = stringMapping[IdToken](IdToken.apply)
  val userId: Mapping[UserId] = Forms.of[Long].transform(l => UserId(l), u => u.id)

  def stringMapping[T <: Wrapped](build: String => T): Mapping[T] =
    Forms.of[String].transform(s => build(s), t => t.value)
}
