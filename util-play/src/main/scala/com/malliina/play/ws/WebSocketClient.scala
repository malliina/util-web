package com.malliina.play.ws

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

/**
 * @author Michael
 */
case class WebSocketClient(user: String, channel: Channel[JsValue], request: RequestHeader)
  extends SocketClient[JsValue]
