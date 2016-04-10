package com.malliina.oauth

import java.nio.file.{Path, Paths}

import com.malliina.file.StorageFile
import com.malliina.util.BaseConfigReader

class GoogleOAuthReader extends BaseConfigReader[GoogleOAuthCredentials] {
  val defaultHomePath = userHome / "keys" / "google-oauth.txt"

  override def filePath: Option[Path] = Option(sys.props.get("google.oauth").map(Paths.get(_)) getOrElse defaultHomePath)

  override def fromMapOpt(map: Map[String, String]): Option[GoogleOAuthCredentials] = for {
    clientId <- map get "clientId"
    clientSecret <- map get "clientSecret"
    scope <- map get "scope"
  } yield GoogleOAuthCredentials(clientId, clientSecret, scope)

}

object GoogleOAuthReader extends GoogleOAuthReader
