package com.malliina.play.ws

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.malliina.play.ws.Mediator.{Broadcast, ClientJoined, ClientLeft, ClientMessage}
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

case class SimpleClientContext(out: ActorRef, rh: RequestHeader, mediator: ActorRef)
  extends ClientContext

trait ClientContext {
  def out: ActorRef

  def rh: RequestHeader

  def mediator: ActorRef
}

class ClientActor(ctx: ClientContext) extends JsonActor(ctx.rh) {
  val mediator = ctx.mediator

  override def preStart(): Unit =
    mediator ! Mediator.ClientJoined(ctx.out)

  override def onMessage(message: JsValue): Unit =
    mediator ! Mediator.ClientMessage(message, rh)
}

object ClientActor {
  def props(ctx: ClientContext) = Props(new ClientActor(ctx))
}

class JsonActor(val rh: RequestHeader) extends Actor with ActorLogging {
  override def receive: Receive = {
    case json: JsValue => onMessage(json)
  }

  def onMessage(message: JsValue): Unit =
    log.info(s"Client $address says: $message")

  def address: String = rh.headers.get(HeaderNames.X_FORWARDED_FOR) getOrElse rh.remoteAddress
}


class ForwardingMediator(sink: ActorRef) extends Mediator {
  override def onClientMessage(message: JsValue, rh: RequestHeader): Unit =
    sink ! Broadcast(message)
}

object ForwardingMediator {
  def props(sink: ActorRef) = Props(new ForwardingMediator(sink))
}

class SelfMediator extends Mediator {
  override def onClientMessage(message: JsValue, rh: RequestHeader): Unit =
    self ! Broadcast(message)
}

object SelfMediator {
  def props() = Props(new SelfMediator)
}

/** To broadcast a message to all connected clients,
  * send a `Broadcast(message)` to this actor.
  *
  * Calls `onClientMessage` when a message is received
  * from a client.
  */
class Mediator extends Actor with ActorLogging {
  var clients: Set[ActorRef] = Set.empty

  override def receive: Receive = {
    case Broadcast(message) =>
      clients foreach { out => out ! message }
    case ClientMessage(message, rh) =>
      onClientMessage(message, rh)
    case ClientJoined(ref) =>
      context watch ref
      clients += ref
      onJoined(ref)
    case ClientLeft(ref) =>
      clients -= ref
      onLeft(ref)
    case Terminated(ref) =>
      clients -= ref
      onLeft(ref)
  }

  def onJoined(ref: ActorRef): Unit = ()

  def onLeft(ref: ActorRef): Unit = ()

  def onClientMessage(message: JsValue, rh: RequestHeader): Unit = ()
}

object Mediator {
  def props() = Props(new Mediator)

  sealed trait Message

  case class ClientJoined(ref: ActorRef) extends Message

  case class ClientLeft(ref: ActorRef) extends Message

  case class Broadcast(json: JsValue) extends Message

  case class ClientMessage(json: JsValue, rh: RequestHeader) extends Message

}
