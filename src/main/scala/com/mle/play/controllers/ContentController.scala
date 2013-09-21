package com.mle.play.controllers

import play.api.http.MimeTypes
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.templates.Html

trait ContentController extends Controller with BaseController {

  def respondResult(html: => SimpleResult, json: => SimpleResult)(implicit request: RequestHeader): SimpleResult = {
    val maybeForceJson = request.getQueryString("f").map(_ == "json")
    if (maybeForceJson.isDefined) {
      NoCache(json)
    } else {
      respondIgnoreQueryParam(html, NoCache(json))
    }
  }

  def respond(html: => SimpleResult, json: => JsValue)(implicit request: RequestHeader): SimpleResult =
    respondResult(html, Ok(json))

  /**
   * Browsers may "Accept" anything, so the HTML option is first.
   *
   * Otherwise you might send JSON to a browser that also accepts HTML.
   *
   * @param html
   * @param json
   * @param request
   * @return
   */
  private def respondIgnoreQueryParam(html: => SimpleResult, json: => SimpleResult)(implicit request: RequestHeader): SimpleResult = {
    if (request accepts MimeTypes.HTML) {
      html
    } else if (request accepts MimeTypes.JSON) {
      json
    } else {
      NotAcceptable
    }
  }

  def response(html: => Html, json: => JsValue)(implicit request: RequestHeader): SimpleResult =
    respond(Ok(html), json)
}
