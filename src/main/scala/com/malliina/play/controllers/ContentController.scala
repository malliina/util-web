package com.malliina.play.controllers

import play.api.http.MimeTypes
import play.api.libs.json.JsValue
import play.api.mvc._
import play.twirl.api.Html

trait ContentController extends Controller with Caching {

  def respond(request: RequestHeader)(html: => Result, json: => JsValue): Result =
    respondResult(request)(html, Ok(json))

  def respondResult(request: RequestHeader)(html: => Result, json: => Result): Result = {
    val forceJson = request.getQueryString("f").contains("json")
    if (forceJson) NoCache(json)
    else respondIgnoreQueryParam(request)(html, NoCache(json))
  }

  /** Browsers may "Accept" anything, so the HTML option is first.
    *
    * Otherwise you might send JSON to a browser that also accepts HTML.
    */
  private def respondIgnoreQueryParam(request: RequestHeader)(html: => Result, json: => Result): Result = {
    if (request accepts MimeTypes.HTML) html
    else if (request accepts MimeTypes.JSON) json
    else NotAcceptable
  }

  def response(request: RequestHeader)(html: => Html, json: => JsValue): Result =
    respond(request)(Ok(html), json)
}
