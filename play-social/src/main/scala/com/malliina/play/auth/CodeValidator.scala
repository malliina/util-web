package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.http.FullUrls
import com.malliina.web.CallbackValidator
import play.api.libs.json.Reads
import play.api.mvc.RequestHeader

import scala.concurrent.Future

/**
  * @tparam U type of user object, e.g. Username, Email, AppUser, String
  */
trait CodeValidator[U, V]
  extends AuthValidator
  with OAuthValidator[V]
  with CallbackValidator[U]
  with PlaySupport[U] {
  protected def commonAuthParams(authScope: String, rh: RequestHeader): Map[String, String] =
    commonAuthParams(authScope, FullUrls(redirCall, rh), clientConf.clientId)

  def postForm[T: Reads](url: FullUrl, params: Map[String, String]): Future[T] =
    http.postFormAs[T](url, params)

  def postEmpty[T: Reads](
    url: FullUrl,
    headers: Map[String, String],
    params: Map[String, String]
  ): Future[T] =
    http.postFormAs[T](url, params, headers)

  def getJson[T: Reads](url: FullUrl): Future[T] = http.getAs[T](url)
}
