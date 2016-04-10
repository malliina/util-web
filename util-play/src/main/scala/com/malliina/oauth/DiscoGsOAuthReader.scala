package com.malliina.oauth

import java.nio.file.{Path, Paths}

import com.malliina.file.StorageFile
import com.malliina.util.BaseConfigReader

class DiscoGsOAuthReader extends BaseConfigReader[DiscoGsOAuthCredentials] {
  val defaultHomePath = userHome / "keys" / "discogs-oauth.txt"

  override def filePath: Option[Path] = Option(sys.props.get("discogs.oauth").map(Paths.get(_)) getOrElse defaultHomePath)

  override def fromMapOpt(map: Map[String, String]): Option[DiscoGsOAuthCredentials] = for {
    cKey <- map get "consumerKey"
    cSecret <- map get "consumerSecret"
    aToken <- map get "accessToken"
    aTokenSecret <- map get "accessTokenSecret"
  } yield DiscoGsOAuthCredentials(cKey, cSecret, aToken, aTokenSecret)
}

object DiscoGsOAuthReader extends DiscoGsOAuthReader