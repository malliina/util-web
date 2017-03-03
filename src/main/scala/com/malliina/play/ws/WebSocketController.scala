package com.malliina.play.ws

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.{Done, NotUsed}
import com.malliina.concurrent.FutureOps
import com.malliina.play.http.Proxies
import com.malliina.play.ws.WebSocketController.log
import play.api.Logger
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._

import scala.concurrent.Future

abstract class WebSocketController(mat: Materializer, socketQueueSize: Int = 10000) extends WebSocketBase {
  implicit val ec = mat.executionContext

  /** Implement this like `routes.YourController.openSocket()`.
    */
  def openSocketCall: Call

  def wsUrl(request: RequestHeader): String =
    openSocketCall.webSocketURL(Proxies.isSecure(request))(request)

  def broadcast(message: Message): Future[Seq[QueueOfferResult]] =
    clients.flatMap(cs => Future.traverse(cs)(_.channel.offer(message)))

  /** Opens a WebSocket connection.
    *
    * This is the controller for requests to ws://... or wss://... URIs.
    *
    * @return a websocket connection using messages of type Message
    */
  def ws(transformer: WebSocket.MessageFlowTransformer[Message, Message]) =
    wsBase(req => Left(onUnauthorized(req)), transformer)

  protected def wsBase(onFailure: RequestHeader => Either[Result, Flow[Message, Message, NotUsed]],
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

  private def authorizedFlow(user: AuthSuccess, req: RequestHeader): Flow[Message, Message, NotUsed] = {
    val (queue, publisher) = Source.queue[Message](socketQueueSize, OverflowStrategy.backpressure)
      .toMat(Sink.asPublisher(fanout = true))(Keep.both).run()(mat)
    val client = newClient(user, queue, req)
    onConnect(client)
    val sink: Sink[Message, Future[Done]] = Sink.foreach[Message](msg => onMessage(msg, client))
      .mapMaterializedValue(_.andThen { case _ => onDisconnect(client) })
    val welcomeSource = welcomeMessage(client).map(Source.single).getOrElse(Source.empty)
    val source = welcomeSource concat Source.fromPublisher(publisher)
    Flow.fromSinkAndSource(sink, source)
  }

  def onUnauthorized(rh: RequestHeader): Result = {
    log warn s"Unauthorized WebSocket connection attempt from: ${Proxies.realAddress(rh)}"
    Results.Unauthorized
  }

  def welcomeMessage(client: Client): Option[Message] = None
}

object WebSocketController {
  private val log = Logger(getClass)
}
