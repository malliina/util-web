package com.malliina.play.ws

import akka.stream.scaladsl.SourceQueue
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

case class JsonSocketClient[U](user: U, channel: SourceQueue[JsValue], request: RequestHeader)
  extends SocketClient[JsValue]
