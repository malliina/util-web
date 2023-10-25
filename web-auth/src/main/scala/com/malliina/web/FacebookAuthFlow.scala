package com.malliina.web

import cats.effect.Sync
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.Email
import com.malliina.web.Utils.{stringify, urlEncode}

object FacebookAuthFlow:
  def staticConf(conf: AuthConf): StaticConf = StaticConf(
    "public_profile email",
    FullUrl.https("www.facebook.com", "/v6.0/dialog/oauth"),
    FullUrl.https("graph.facebook.com", "/v6.0/oauth/access_token"),
    conf
  )

class FacebookAuthFlow[F[_]: Sync](authConf: AuthConf, http: HttpClient[F])
  extends StaticFlowStart[F]
  with CallbackValidator[F, Email]:
  val brandName = "Facebook"
  val conf: StaticConf = FacebookAuthFlow.staticConf(authConf)

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): F[Either[AuthError, Email]] =
    val params = validationParams(code, redirectUrl, authConf).map { case (k, v) =>
      k -> urlEncode(v)
    }
    val url = conf.tokenEndpoint.append(s"?${stringify(params)}")

    // https://developers.facebook.com/docs/graph-api/explorer/
    for
      tokens <- http.getAs[FacebookTokens](url)
      emailUrl = FullUrl.https(
        "graph.facebook.com",
        s"/v6.0/me?fields=email&access_token=${tokens.accessToken}"
      )
      emailResponse <- http.getAs[EmailResponse](emailUrl)
    yield Right(emailResponse.email)
