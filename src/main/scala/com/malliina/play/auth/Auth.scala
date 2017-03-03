package com.malliina.play.auth

import com.malliina.play.models.{Password, Username}
import org.apache.commons.codec.binary.Base64
import play.api.http.HeaderNames
import play.api.mvc.{RequestHeader, Security}

object Auth {
  def basicCredentials(request: RequestHeader): Option[BasicCredentials] = {
    authHeaderParser(request) { decoded =>
      decoded.split(":", 2) match {
        case Array(user, pass) => Some(BasicCredentials(Username(user), Password(pass)))
        case _ => None
      }
    }
  }

  /**
    *
    * @param request
    * @param f decoded credentials => T
    * @tparam T
    * @return
    */
  def authHeaderParser[T](request: RequestHeader)(f: String => Option[T]): Option[T] = {
    request.headers.get(HeaderNames.AUTHORIZATION) flatMap { authInfo =>
      authInfo.split(" ") match {
        case Array(_, encodedCredentials) =>
          val decoded = new String(Base64.decodeBase64(encodedCredentials.getBytes))
          f(decoded)
        case _ =>
          None
      }
    }
  }

  def credentialsFromQuery(request: RequestHeader, userKey: String = "u", passKey: String = "p"): Option[BasicCredentials] = {
    val qString = request.queryString
    for (
      u <- qString get userKey;
      p <- qString get passKey;
      user <- u.headOption;
      pass <- p.headOption
    ) yield BasicCredentials(Username(user), Password(pass))
  }

  def authenticateFromSession(rh: RequestHeader,
                              sessionKey: String = Security.username): Option[Username] =
    rh.session.get(sessionKey).map(Username.apply)
}
