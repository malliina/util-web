package com.mle.play

import com.mle.util.Log
import play.api.GlobalSettings
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

/**
 *
 * @author mle
 */
trait GlobalErrorHandling extends GlobalSettings with Log {

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    def src = request.remoteAddress
    def path = request.path
    def exName = ex.getClass.getName
    log.warn(s"Unhandled $exName for request to: $path from: $src", ex)
    super.onError(request, ex)
  }
}
