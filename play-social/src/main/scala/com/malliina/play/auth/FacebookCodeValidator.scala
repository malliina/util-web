package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.values.Email
import play.api.mvc.RequestHeader

import scala.concurrent.Future

object FacebookCodeValidator {
  def apply(conf: OAuthConf[Email]) = new FacebookCodeValidator(conf)

  def staticConf(conf: AuthConf) = StaticConf(
    "public_profile email",
    FullUrl.https("www.facebook.com", "/v2.12/dialog/oauth"),
    FullUrl.https("graph.facebook.com", "/v2.12/oauth/access_token"),
    conf
  )
}

class FacebookCodeValidator(val oauth: OAuthConf[Email])
    extends StaticCodeValidator[Email, Email]("Facebook", StaticConf.facebook(oauth.conf))
    with HandlerLike {

  override def validate(code: Code, req: RequestHeader): Future[Either[AuthError, Email]] = {
    val params = validationParams(code, req).map { case (k, v) => k -> urlEncode(v) }
    val url = staticConf.tokenEndpoint.append(s"?${stringify(params)}")

    // https://developers.facebook.com/docs/php/howto/example_retrieve_user_profile
    for {
      tokens <- getJson[FacebookTokens](url)
      emailUrl = FullUrl.https("graph.facebook.com",
                               s"/v2.12/me?fields=email&access_token=${tokens.accessToken}")
      emailResponse <- getJson[EmailResponse](emailUrl)
    } yield Right(emailResponse.email)
  }
}
