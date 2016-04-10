package com.malliina.play.http

import com.malliina.file.FileUtilities
import play.api.mvc.RequestHeader

trait PlayUtils {
  def headersString(request: RequestHeader) =
    request.headers.toSimpleMap
      .map(kv => kv._1 + "=" + kv._2)
      .mkString(FileUtilities.lineSep, FileUtilities.lineSep, "")
}

object PlayUtils extends PlayUtils
