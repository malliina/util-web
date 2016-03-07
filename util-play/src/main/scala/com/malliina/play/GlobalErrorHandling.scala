package com.malliina.play

import com.malliina.play.GlobalErrorHandling.log
import play.api.mvc.{RequestHeader, Result}
import play.api.{GlobalSettings, Logger}

import scala.concurrent.Future

trait GlobalErrorHandling extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    def src = request.remoteAddress
    def path = request.path
    def exName = ex.getClass.getName
    log.warn(s"Unhandled $exName for request to: $path from: $src", ex)
    super.onError(request, ex)
  }
}

object GlobalErrorHandling {
  private val log = Logger(getClass)
}
