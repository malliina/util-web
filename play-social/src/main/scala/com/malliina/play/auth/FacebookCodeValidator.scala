package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.values.Email
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.Future

class FacebookCodeValidator(val redirCall: Call,
                            val handler: AuthHandler,
                            conf: AuthConf,
                            val http: OkClient)
  extends StaticCodeValidator[Email]("Facebook", StaticConf.facebook(conf)) with HandlerLike {

  override def validate(code: Code, req: RequestHeader): Future[Either[AuthError, Email]] = {
    val params = validationParams(code, req).mapValues(urlEncode)
    val url = staticConf.tokenEndpoint.append(s"?${stringify(params)}")

    getJson[FacebookTokens](url).flatMapRight { tokens =>
      // https://developers.facebook.com/docs/php/howto/example_retrieve_user_profile
      val emailUrl = FullUrl.https("graph.facebook.com", s"/v2.12/me?fields=email&access_token=${tokens.accessToken}")
      getJson[EmailResponse](emailUrl).mapR(_.email)
    }
  }
}
