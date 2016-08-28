package com.malliina.play.controllers

import com.malliina.play.http.HttpConstants
import play.api.http.HeaderNames._
import play.api.http.Writeable
import play.api.mvc.{Result, Results}

trait BaseController {
  def NoCacheOk[C: Writeable](content: C) =
    NoCache(Results.Ok(content))

  def NoCache[T](result: => Result): Result =
    result.withHeaders(CACHE_CONTROL -> HttpConstants.NoCache)
}

object BaseController extends BaseController
