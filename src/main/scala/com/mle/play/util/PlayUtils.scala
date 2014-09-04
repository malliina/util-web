package com.mle.play.util

import com.mle.util.Log
import play.api.mvc.RequestHeader

/**
 *
 * @author mle
 */
trait PlayUtils extends Log {
  def formatHeaders(req: RequestHeader) =
    req.headers.toMap.map {
      case (key, values) => s"$key : ${values.mkString(",")}"
    }.mkString("\n", "\n", "")

  def logHeaders(req: RequestHeader) = log info formatHeaders(req)
}

object PlayUtils extends PlayUtils