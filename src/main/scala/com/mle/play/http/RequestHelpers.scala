package com.mle.play.http

import play.api.mvc.RequestHeader
import java.net.InetSocketAddress
import play.core.server.NettyServer
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.Channel
import com.mle.util.{Util, Log}

/**
 * The main motivation of this class is to be able to find out
 * whether a request is made over HTTPS or not.
 */
class RequestHelpers(serverOpt: Option[NettyServer]) extends Log {
  /**
   * May not work if the HTTPS port is 80 and excluded from the request's <code>host</code> member.
   *
   * @param request the incoming request
   * @return true if the request was made over HTTPS, false otherwise
   */
  def isHttps(request: RequestHeader): Boolean =
    httpsPort.exists(sslPort => request.host.contains(s":$sslPort"))

  def httpsPort = port(_.HTTPS)

  def httpPort = port(_.HTTP)

  def port(req: RequestHeader): Int =
    (if (isHttps(req)) httpsPort else httpPort) orElse portFromHost(req) getOrElse 80

  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.size > 1) Util.optionally[Int, NumberFormatException](maybeSuffix.tail.toInt)
    else None
  }

  private def port(f: NettyServer => Option[(ServerBootstrap, Channel)]): Option[Int] =
    for {
      server <- serverOpt
      serverAndChannel <- f(server)
      localAddr <- Option(serverAndChannel._2.getLocalAddress)
    } yield localAddr.asInstanceOf[InetSocketAddress].getPort
}