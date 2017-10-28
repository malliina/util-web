package com.malliina.oauth

trait GoogleOAuthKey {
  def clientId: String

  def clientSecret: String
}

case class GoogleOAuthCredentials(clientId: String,
                                  clientSecret: String,
                                  scope: String) extends GoogleOAuthKey

case class GoogleOAuthCreds(clientId: String,
                            clientSecret: String) extends GoogleOAuthKey
