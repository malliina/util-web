package com.mle.oauth

import java.nio.file.{Path, Paths}

import com.mle.file.StorageFile
import com.mle.util.BaseConfigReader

/**
 * @author Michael
 */
class GoogleOAuthReader extends BaseConfigReader[GoogleOAuthCredentials] {
  val defaultHomePath = userHome / "keys" / "google-oauth.txt"

  override def resourceCredential: String = ""

  override def userHomeConfPath: Path = sys.props.get("google.oauth").map(Paths.get(_)) getOrElse defaultHomePath

  override def fromMapOpt(map: Map[String, String]): Option[GoogleOAuthCredentials] = for {
    clientId <- map get "clientId"
    clientSecret <- map get "clientSecret"
    scope <- map get "scope"
  } yield GoogleOAuthCredentials(clientId, clientSecret, scope)

}

object GoogleOAuthReader extends GoogleOAuthReader