package com.malliina.play.auth

import com.malliina.http.OkClient
import play.api.mvc.Call

case class OAuthConf[U](redirCall: Call, handler: AuthResults[U], conf: AuthConf, http: OkClient)

case class AuthCodeConf(
  brandName: String,
  redirCall: Call,
  conf: AuthConf,
  client: KeyClient,
  extraStartParams: Map[String, String] = Map.empty,
  extraValidateParams: Map[String, String] = Map.empty
)

case class CodeValidationConf[U](oauth: OAuthConf[U], codeConf: AuthCodeConf) {
  def brandName = codeConf.brandName
  def handler = oauth.handler
  def conf: AuthConf = oauth.conf
  def client: KeyClient = codeConf.client
  def redirCall = oauth.redirCall
  def extraStartParams = codeConf.extraStartParams
  def extraValidateParams = codeConf.extraValidateParams
}
