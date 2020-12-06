package com.malliina.play.auth

import com.malliina.http.OkClient
import com.malliina.web.{ClientId, GoogleAuthFlow, KeyClient, MicrosoftAuthFlow}

object KeyClient {
  def microsoft(clientIds: Seq[ClientId], http: OkClient): KeyClient =
    MicrosoftAuthFlow.keyClient(clientIds, http)

  def google(clientIds: Seq[ClientId], http: OkClient): KeyClient =
    GoogleAuthFlow.keyClient(clientIds, http)
}
