package com.malliina.play.http

import java.util.regex.Pattern

import com.malliina.play.json.ValidatingCompanion
import play.api.mvc.{Call, RequestHeader}

case class FullUrl(proto: String, hostAndPort: String, uri: String) {
  val host = hostAndPort.takeWhile(_ != ':')
  val protoAndHost = s"$proto://$hostAndPort"
  val url = s"$protoAndHost$uri"

  def absolute(call: Call): FullUrl = {
    val fragment = Option(call.fragment)
      .filter(_.trim.nonEmpty)
      .map(f => s"#$f")
      .getOrElse("")
    val callUri = s"${call.url}$fragment"
    FullUrl(proto, hostAndPort, callUri)
  }

  def /(more: String) = append(more.dropWhile(_ == '/'))

  def +(more: String) = append(more)

  def append(more: String): FullUrl = FullUrl(proto, hostAndPort, s"$uri$more")

  override def toString: String = url
}

object FullUrl extends ValidatingCompanion[String, FullUrl] {
  val urlPattern = Pattern compile """(.+)://([^/]+)(/?.*)"""

  def apply(call: Call, request: RequestHeader) =
    hostOnly(request).absolute(call)

  /** Ignores the uri of `request`.
    *
    * @param rh source
    * @return a url of the host component of `request`
    */
  def hostOnly(rh: RequestHeader): FullUrl = {
    val maybeS = if (Proxies.isSecure(rh)) "s" else ""
    FullUrl(s"http$maybeS", rh.host, "")
  }

  override def build(input: String): Option[FullUrl] = {
    val m = urlPattern.matcher(input)
    if (m.find() && m.groupCount() == 3) {
      Option(FullUrl(m group 1, m group 2, m group 3))
    } else {
      None
    }
  }

  override def write(t: FullUrl): String = t.url
}
