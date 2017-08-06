package com.malliina.oauth

import com.malliina.play.http.FullUrl
import com.malliina.play.models.Email

import scala.concurrent.Future

trait GoogleOAuthLike extends AutoCloseable {
  def discover(): Future[GoogleOAuthConf]

  def authRequestUri(authEndpoint: FullUrl, redirectUri: FullUrl, state: String): FullUrl

  def resolveEmail(tokenEndpoint: FullUrl, code: String, redirectUri: FullUrl): Future[Email]
}
