package com.mle.play.controllers

import play.api.mvc.{Result, Results}
import play.api.http.HeaderNames._
import com.mle.play.http.HttpConstants
import HttpConstants._

/**
 *
 * @author mle
 */
trait BaseController {
  def NoCacheOk[C](content: C)(implicit writeable: play.api.http.Writeable[C]) =
    NoCache(Results.Ok(content))

  def NoCache[T](result: => Result): Result =
    result.withHeaders(CACHE_CONTROL -> NO_CACHE)
}

object BaseController extends BaseController
