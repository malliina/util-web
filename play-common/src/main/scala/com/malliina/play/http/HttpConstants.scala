package com.malliina.play.http

trait HttpConstants {
  val AudioMpeg = "audio/mpeg"
  val NoCache = "no-cache"
  val NoCacheRevalidate = "no-cache, no-store, must-revalidate"
}

object HttpConstants extends HttpConstants
