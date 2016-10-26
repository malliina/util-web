package com.malliina.play.ws

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import play.api.mvc.RequestHeader
import play.api.mvc.WebSocket.MessageFlowTransformer

trait LegacySockets {
  self: WebSocketController =>

  /** Instead of returning an Unauthorized result upon authentication failures, this opens then immediately closes a
    * connection connections, sends no messages and ignores any messages.
    *
    * The Java-WebSocket client library hangs if an Unauthorized result is returned after a websocket connection attempt.
    *
    * @return a websocket connection using messages of type Message
    */
  def ws2(transformer: MessageFlowTransformer[Message, Message]) =
    wsBase(req => Right(unauthorizedFlow(req)), transformer)

  private def unauthorizedFlow(req: RequestHeader): Flow[Any, Nothing, NotUsed] = {
    onUnauthorized(req)
    Flow.fromSinkAndSource(Sink.ignore, Source.empty)
  }

}
