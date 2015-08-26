package com.mle.play.http

import com.mle.file.FileUtilities
import play.api.mvc.RequestHeader

/**
 *
 * @author mle
 */
trait PlayUtils {
  def headersString(request: RequestHeader) =
    request.headers.toSimpleMap
      .map(kv => kv._1 + "=" + kv._2)
      .mkString(FileUtilities.lineSep, FileUtilities.lineSep, "")
}

object PlayUtils extends PlayUtils
