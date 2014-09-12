package com.mle.play.ws

import com.mle.play.controllers.AuthResult
import com.mle.util.Log
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.mvc.{RequestHeader, Result, Results, WebSocket}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * @author mle
 */
trait WebSocketController extends WebSocketBase with Log {
  /**
   * Opens a WebSocket connection.
   *
   * This is the controller for requests to ws://... or wss://... URIs.
   *
   * The implementation is problematic because a websocket connection appears to be opened even if authentication fails,
   * if only for a very brief moment. I would prefer to return some erroneous HTTP status code when authentication fails
   * but I don't know how to.
   *
   * @return a websocket connection using messages of type Message
   */
  @deprecated("Use ws3(...)", "1.5.4")
  def ws(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    ws2(welcomeMessage.map(Enumerator[Message](_)).getOrElse(Enumerator.empty[Message]))

  @deprecated("Use ws3(...)", "1.5.4")
  def ws2(initialEnumerator: Enumerator[Message])(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.using[Message](request => {
      authenticate(request).map(user => {
        val (out, channel) = Concurrent.broadcast[Message]
        val clientInfo: Client = newClient(user.user, channel)(request)
        onConnect(clientInfo)
        // iteratee that eats client messages (input)
        val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
        val enumerator = Enumerator.interleave(initialEnumerator, out)
        (in, enumerator)
      }).getOrElse({
        // authentication failed
        log warn s"Unauthorized WebSocket connection attempt from: ${request.remoteAddress}"
        val in = Iteratee.foreach[Message](_ => ())
        val out = Enumerator.eof[Message]
        (in, out)
      })
    })

  def ws3(implicit frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.tryAccept[Message](req => Future.successful {
      authenticate(req).fold[Either[Result, (Iteratee[Message, _], Enumerator[Message])]](Left(Results.Unauthorized))(user => {
        val (out, channel) = Concurrent.broadcast[Message]
        val clientInfo: Client = newClient(user.user, channel)(req)
        onConnect(clientInfo)
        // iteratee that eats client messages (input)
        val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
        val outEnumerator = Enumerator.interleave(welcomeEnumerator, out)
        Right((in, outEnumerator))
      })
    })

  def welcomeMessage: Option[Message] = None

  def welcomeEnumerator = welcomeMessage.map(Enumerator[Message](_)).getOrElse(Enumerator.empty[Message])

  def authenticate(implicit request: RequestHeader): Option[AuthResult]
}
