package com.mle.play

import play.api.GlobalSettings
import play.api.mvc.{SimpleResult, RequestHeader}
import scala.concurrent.Future
import com.mle.util.Log

/**
 *
 * @author mle
 */
trait GlobalErrorHandling extends GlobalSettings with Log {
  override def onError(request: RequestHeader, ex: Throwable): Future[SimpleResult] = {
    def src = request.remoteAddress
    def path = request.path
    def exName = ex.getClass.getName
    log.warn(s"Unhandled $exName for request to: $path from: $src", ex)
    super.onError(request, ex)
  }
}
