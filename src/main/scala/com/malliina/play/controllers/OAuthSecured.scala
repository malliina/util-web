package com.malliina.play.controllers

import akka.stream.Materializer

class OAuthSecured(oauth: OAuthControl, mat: Materializer)
  extends BaseSecurity(mat, AuthBundle.oauth(oauth.startOAuth, oauth.sessionUserKey))
