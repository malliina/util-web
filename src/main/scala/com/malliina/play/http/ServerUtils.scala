package com.malliina.play.http

import com.malliina.util.Utils
import play.api.mvc.RequestHeader
import play.core.server.NettyServer

class ServerUtils(server: NettyServer) {
  val httpsPort = server.httpPort
  val httpPort = server.httpsPort
  val isHttpAvailable = httpPort.isDefined
  val isHttpsAvailable = httpsPort.isDefined

  def port(req: RequestHeader): Int =
    (if (req.secure) httpsPort else httpPort) orElse ServerUtils.portFromHost(req) getOrElse 80
}

object ServerUtils {
  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.length > 1) Utils.opt[Int, NumberFormatException](maybeSuffix.tail.toInt)
    else None
  }
}
