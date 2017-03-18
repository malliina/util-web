package com.malliina.play.controllers

import akka.stream.Materializer

class OAuthSecured(oauth: OAuthControl, mat: Materializer)
  extends BaseSecurity(AuthBundle.oauth(oauth.startOAuth, oauth.sessionUserKey), mat)
