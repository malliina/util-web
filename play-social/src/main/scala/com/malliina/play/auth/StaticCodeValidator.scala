package com.malliina.play.auth

import com.malliina.play.http.FullUrls
import com.malliina.web.{StaticConf, StaticFlowStart}
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

/** A validator where the authorization and token endpoints are static, that is, no discovery endpoint is used.
  *
  * @param brandName  provider name
  * @param conf conf
  */
abstract class StaticCodeValidator[U, V](val brandName: String, val conf: StaticConf)
  extends CodeValidator[U, V]
  with StaticFlowStart {

  override def start(
    req: RequestHeader,
    extraParams: Map[String, String] = Map.empty
  ): Future[Result] = {
    implicit val ec = http.exec
    start(FullUrls(redirCall, req), extraParams).map { s => redirResult(s.authorizationEndpoint, s.params, s.nonce) }
  }
}
