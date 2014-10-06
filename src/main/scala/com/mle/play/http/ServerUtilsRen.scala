package com.mle.play.http

import java.net.InetSocketAddress

import com.mle.util.{Log, Utils}
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.Channel
import play.api.mvc.RequestHeader
import play.core.server.NettyServer

class ServerUtils(server: NettyServer) extends Log {
  val httpsPort = port(_.HTTPS)
  val httpPort = port(_.HTTP)
  val isHttpAvailable = httpPort.isDefined
  val isHttpsAvailable = httpsPort.isDefined

  /**
   * May not work if the HTTPS port is 80 and excluded
   * from the request's <code>host</code> member.
   *
   * @param request the incoming request
   * @return true if the request was made over HTTPS, false otherwise
   */
  def isHttps(request: RequestHeader): Boolean =
    httpsPort.exists(sslPort => request.host.contains(s":$sslPort"))

  def port(req: RequestHeader): Int =
    (if (isHttps(req)) httpsPort else httpPort) orElse ServerUtils.portFromHost(req) getOrElse 80

  private def port(f: NettyServer => Option[(ServerBootstrap, Channel)]): Option[Int] =
    for {
      fOpt <- f(server)
      localAddr <- Option(fOpt._2.getLocalAddress)
    } yield localAddr.asInstanceOf[InetSocketAddress].getPort
}

object ServerUtils {
  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.size > 1) Utils.opt[Int, NumberFormatException](maybeSuffix.tail.toInt)
    else None
  }
}
