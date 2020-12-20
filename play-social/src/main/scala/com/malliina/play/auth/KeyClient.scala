package com.malliina.play.auth

import cats.effect.IO
import com.malliina.http.HttpClient
import com.malliina.web.{ClientId, GoogleAuthFlow, KeyClient, MicrosoftAuthFlow}

object KeyClient {
  def microsoft(clientIds: Seq[ClientId], http: HttpClient[IO]): KeyClient =
    MicrosoftAuthFlow.keyClient(clientIds, http)

  def google(clientIds: Seq[ClientId], http: HttpClient[IO]): KeyClient =
    GoogleAuthFlow.keyClient(clientIds, http)
}
