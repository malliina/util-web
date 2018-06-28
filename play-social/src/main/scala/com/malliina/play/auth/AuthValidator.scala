package com.malliina.play.auth

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object AuthValidator {
  def urlEncode(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.name())
}

trait AuthValidator {
  implicit val ec = Execution.cached

  def brandName: String

  /** The initial result that initiates sign-in.
    */
  def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): Future[Result]

  /** The callback in the auth flow, i.e. the result for redirect URIs.
    */
  def validateCallback(req: RequestHeader): Future[Result]

  protected def stringify(map: Map[String, String]) =
    map.map { case (key, value) => s"$key=$value" }.mkString("&")

  protected def fut[T](t: T): Future[T] = Future.successful(t)
}
