package com.malliina.play.ws

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.malliina.play.ws.Mediator.{Broadcast, ClientJoined, ClientLeft}
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
  override def preStart(): Unit = ctx.mediator ! Mediator.ClientJoined(ctx.out)
}

object ClientActor {
  def props(ctx: ClientContext) = Props(new ClientActor(ctx))
}

class JsonActor(rh: RequestHeader) extends Actor with ActorLogging {
  override def receive: Receive = {
    case json: JsValue => onMessage(json)
  }

  def onMessage(message: JsValue): Unit =
    log.info(s"Client $address says: $message")

  def address: String = rh.headers.get(HeaderNames.X_FORWARDED_FOR) getOrElse rh.remoteAddress
}

class Mediator extends Actor with ActorLogging {
  var clients: Set[ActorRef] = Set.empty

  override def receive: Receive = {
    case Broadcast(message) =>
      clients foreach { out => out ! message }
    case ClientJoined(ref) =>
      context watch ref
      clients += ref
    case ClientLeft(ref) =>
      clients -= ref
    case Terminated(ref) =>
      clients -= ref
  }
}

object Mediator {
  def props() = Props(new Mediator)

  sealed trait Message

  case class ClientJoined(ref: ActorRef) extends Message

  case class ClientLeft(ref: ActorRef) extends Message

  case class Broadcast(json: JsValue) extends Message

}
