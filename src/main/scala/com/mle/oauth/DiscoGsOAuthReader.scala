package com.mle.oauth

import java.nio.file.{Path, Paths}

import com.mle.file.StorageFile
import com.mle.util.BaseConfigReader

/**
 * @author Michael
 */
class DiscoGsOAuthReader extends BaseConfigReader[DiscoGsOAuthCredentials] {
  val defaultHomePath = userHome / "keys" / "discogs-oauth.txt"

  override def userHomeConfPath: Path = sys.props.get("discogs.oauth").map(Paths.get(_)) getOrElse defaultHomePath

  override def fromMapOpt(map: Map[String, String]): Option[DiscoGsOAuthCredentials] = for {
    cKey <- map get "consumerKey"
    cSecret <- map get "consumerSecret"
    aToken <- map get "accessToken"
    aTokenSecret <- map get "accessTokenSecret"
  } yield DiscoGsOAuthCredentials(cKey, cSecret, aToken, aTokenSecret)
}

object DiscoGsOAuthReader extends DiscoGsOAuthReader