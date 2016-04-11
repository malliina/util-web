package com.malliina.play.ws

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.{Done, NotUsed}
import com.malliina.concurrent.FutureOps
import com.malliina.play.ws.WebSocketController.log
import play.api.Logger
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait WebSocketController extends WebSocketBase {
  val BufferSize = 10000

  implicit def mat: Materializer

  /** Implement this like `routes.YourController.openSocket()`.
    */
  def openSocketCall: Call

  def wsUrl(implicit request: RequestHeader): String =
    openSocketCall.webSocketURL(request.secure)

  def broadcast(message: Message): Future[Seq[QueueOfferResult]] =
    Future.traverse(clients)(_.channel.offer(message))

  /** Opens a WebSocket connection.
    *
    * This is the controller for requests to ws://... or wss://... URIs.
    *
    * @return a websocket connection using messages of type Message
    */
  def ws(transformer: WebSocket.MessageFlowTransformer[Message, Message]) =
    wsBase(req => Left(onUnauthorized(req)), transformer)

  /** Instead of returning an Unauthorized result upon authentication failures, this opens then immediately closes a
    * connection connections, sends no messages and ignores any messages.
    *
    * The Java-WebSocket client library hangs if an Unauthorized result is returned after a websocket connection attempt.
    *
    * @return
    */
  def ws2(transformer: MessageFlowTransformer[Message, Message]) =
    wsBase(req => Right(unauthorizedFlow(req)), transformer)

  private def wsBase(onFailure: RequestHeader => Either[Result, Flow[Message, Message, NotUsed]],
                     transformer: MessageFlowTransformer[Message, Message]) =
    WebSocket.acceptOrResult[Message, Message] { request =>
      authenticateAsync(request)
        .map(res => Right(authorizedFlow(res, request)))
        .recoverAll(t => onFailure(request))
    }(transformer)

  /** What do we want?
    * - Future[Either[AuthFailure, AuthSuccess]]
    * - Future[Option[AuthSuccess]]
    * - Future[AuthSuccess]
    *
    * IMO: The first one, then the others as convenience based on the first. (_.toOption, _.toOption.get)
    *
    * @param req req
    * @return a successful authentication result, or fails with a NoSuchElementException if authentication fails
    */
  def authenticateAsync(req: RequestHeader): Future[AuthSuccess]

  //  private def authorizedSocket(user: AuthSuccess, req: RequestHeader): (Iteratee[Message, _], Enumerator[Message]) = {
  //    val (out, channel) = Concurrent.broadcast[Message]
  //    val clientInfo: Client = newClient(user, channel)(req)
  //    onConnect(clientInfo)
  //    // iteratee that eats client messages (input)
  //    val in = Iteratee.foreach[Message](msg => onMessage(msg, clientInfo)).map(_ => onDisconnect(clientInfo))
  //    val enumerator = Enumerator.interleave(welcomeEnumerator(clientInfo), out)
  //    (in, enumerator)
  //  }

  private def authorizedFlow(user: AuthSuccess, req: RequestHeader): Flow[Message, Message, NotUsed] = {
    val (queue, publisher) = Source.queue[Message](BufferSize, OverflowStrategy.backpressure)
      .toMat(Sink.asPublisher(fanout = true))(Keep.both).run()
    val client = newClient(user, queue)(req)
    onConnect(client)
    val sink: Sink[Message, Future[Done]] = Sink.foreach[Message](msg => onMessage(msg, client))
      .mapMaterializedValue(_.andThen { case _ => onDisconnect(client) })
    val welcomeSource = welcomeMessage(client).map(Source.single).getOrElse(Source.empty)
    val source = welcomeSource concat Source.fromPublisher(publisher)
    Flow.fromSinkAndSource(sink, source)
  }

  private def unauthorizedFlow(req: RequestHeader): Flow[Any, Nothing, NotUsed] = {
    onUnauthorized(req)
    Flow.fromSinkAndSource(Sink.ignore, Source.empty)
  }

  def onUnauthorized(req: RequestHeader): Result = {
    log warn s"Unauthorized WebSocket connection attempt from: ${req.remoteAddress}"
    Results.Unauthorized
  }

  def welcomeMessage(client: Client): Option[Message] = None
}

object WebSocketController {
  private val log = Logger(getClass)
}
