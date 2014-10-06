package com.mle.play.ws

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.util.Log
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * @author mle
 */
trait WebSocketController extends WebSocketBase with Log {
  /**
   * Implement this like `routes.YourController.openSocket()`.
   *
   * @return
   */
  def openSocketCall: Call

  def wsUrl(implicit request: RequestHeader): String = openSocketCall.webSocketURL(request.secure)

  def broadcast(message: Message) = clients.foreach(_.channel push message)

  /**
   * Opens a WebSocket connection.
   *
   * This is the controller for requests to ws://... or wss://... URIs.
   *
   * @return a websocket connection using messages of type Message
   */
  def ws(frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    wsBase(req => Left(onUnauthorized(req)), frameFormatter)

  /**
   * Instead of returning an Unauthorized result upon authentication failures, this opens then immediately closes a
   * connection connections, sends no messages and ignores any messages.
   *
   * The Java-WebSocket client library hangs if an Unauthorized result is returned after a websocket connection attempt.
   *
   * @return
   */
  def ws2(frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    wsBase(req => Right(unauthorizedSocket(req)), frameFormatter)

  private def wsBase(onFailure: RequestHeader => Either[Result, (Iteratee[Message, _], Enumerator[Message])],
                     frameFormatter: WebSocket.FrameFormatter[Message]): WebSocket[Message, Message] =
    WebSocket.tryAccept[Message](request => {
      authenticateAsync(request)
        .map(res => Right(authorizedSocket(res, request)))
        .recoverAll(t => onFailure(request))
    })(frameFormatter)

  /**
   * What do we want?
   * - Future[Either[AuthFailure, AuthSuccess]]
   * - Future[Option[AuthSuccess]]
   * - Future[AuthSuccess]
   *
   * @param req req
   * @return a successful authentication result, or fails with a NoSuchElementException if authentication fails
   */
  def authenticateAsync(req: RequestHeader): Future[AuthSuccess]

  private def authorizedSocket(user: AuthSuccess, req: RequestHeader): (Iteratee[Message, _], Enumerator[Message]) = {
    val (out, channel) = Concurrent.broadcast[Message]
    val clientInfo: Client = newClient(user, channel)(req)
    onConnect(clientInfo)
    // iteratee that eats client messages (input)
    val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
    val enumerator = Enumerator.interleave(welcomeEnumerator(clientInfo), out)
    (in, enumerator)
  }

  private def unauthorizedSocket(req: RequestHeader): (Iteratee[Message, _], Enumerator[Message]) = {
    onUnauthorized(req)
    val in = Iteratee.foreach[Message](_ => ())
    val out = Enumerator.eof[Message]
    (in, out)
  }

  def onUnauthorized(req: RequestHeader) = {
    log warn s"Unauthorized WebSocket connection attempt from: ${req.remoteAddress}"
    Results.Unauthorized
  }

  def welcomeMessage(client: Client): Option[Message] = None

  def welcomeEnumerator(client: Client) = toEnumerator(welcomeMessage(client))

  def toEnumerator(maybeMessage: Option[Message]) =
    maybeMessage.map(Enumerator[Message](_)).getOrElse(Enumerator.empty[Message])
}
