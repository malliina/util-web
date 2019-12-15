package com.malliina.play.auth

import com.malliina.values.{Email, Username}

trait JWTUser {
  def username: Username
}

case class CognitoUser(
  username: Username,
  email: Option[Email],
  groups: Seq[String],
  verified: Verified
) extends JWTUser
