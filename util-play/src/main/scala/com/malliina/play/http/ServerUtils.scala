package com.malliina.play.http

import com.malliina.util.Utils
import play.api.mvc.RequestHeader

object ServerUtils {
  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.length > 1) Utils.opt[Int, NumberFormatException](maybeSuffix.tail.toInt)
    else None
  }
}