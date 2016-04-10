package com.malliina.play.controllers

import com.malliina.play.http.HttpConstants.NO_CACHE
import play.api.http.HeaderNames._
import play.api.http.Writeable
import play.api.mvc.{Result, Results}

trait BaseController {
  def NoCacheOk[C](content: C)(implicit writeable: Writeable[C]) =
    NoCache(Results.Ok(content))

  def NoCache[T](result: => Result): Result =
    result.withHeaders(CACHE_CONTROL -> NO_CACHE)
}

object BaseController extends BaseController
