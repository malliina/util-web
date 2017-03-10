package com.malliina.play.ws

import akka.actor.{Actor, ActorRef, Cancellable, PoisonPill, Props, Terminated}
import com.malliina.collections.BoundedList
import com.malliina.play.http.Proxies
import com.malliina.play.json.JsonMessages
import com.malliina.play.ws.Mediator.{Broadcast, ClientJoined, ClientLeft, ClientMessage}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration.DurationInt

case class MediatorClient(ctx: ActorMeta, mediator: ActorRef)
  extends ClientContext {
  override def out = ctx.out

  override def rh = ctx.rh
}

trait ClientContext extends ActorMeta {
  def mediator: ActorRef
}

case class ActorInfo(out: ActorRef, rh: RequestHeader) extends ActorMeta

trait ActorMeta {
  def out: ActorRef

  def rh: RequestHeader
}

case class DefaultActorConfig[U](out: ActorRef, rh: RequestHeader, user: U)
  extends ActorConfig[U]

trait ActorConfig[U] extends ActorMeta {
  def user: U
}

class ObserverActor(events: Observable[JsValue], ctx: ActorMeta) extends JsonActor(ctx) {
  var subscription: Option[Subscription] = None

  override def preStart() = {
    super.preStart()
    val sub = events.subscribe(
      json => ctx.out ! json,
      _ => self ! PoisonPill,
      () => self ! PoisonPill
    )
    subscription = Option(sub)
  }

  override def postStop() = {
    super.postStop()
    subscription foreach { sub => sub.unsubscribe() }
    subscription = None
  }
}

class ClientActor(ctx: ClientContext) extends JsonActor(ctx) {
  val mediator = ctx.mediator

  override def preStart(): Unit =
    mediator ! Mediator.ClientJoined(ctx.out)

  override def onMessage(message: JsValue): Unit = {
    transform(message).fold(
      error => ClientActor.log.error(s"Validation of '$message' failed. $error"),
      json => mediator ! ClientMessage(json, rh)
    )
  }

  /** Transforms an incoming message before sending it to the mediator.
    *
    * @param message incoming message
    * @return the transformed message, or an error message
    */
  def transform(message: JsValue): JsResult[JsValue] = JsSuccess(message)
}

object ClientActor {
  private val log = Logger(getClass)

  def props(ctx: ClientContext) = Props(new ClientActor(ctx))
}

class JsonActor(ctx: ActorMeta) extends Actor {
  implicit val ec = context.dispatcher
  val out = ctx.out
  val rh = ctx.rh
  var pinger: Option[Cancellable] = None

  override def preStart() = {
    super.preStart()
    val healthCheck = context.system.scheduler.schedule(
      initialDelay = 10.seconds,
      interval = 30.seconds,
      receiver = out,
      message = JsonMessages.ping
    )
    pinger = Option(healthCheck)
  }

  override def receive: Receive = {
    case json: JsValue => onMessage(json)
  }

  def onMessage(message: JsValue): Unit =
    JsonActor.log.info(s"Client '$address' says '$message'.")

  def address: String = Proxies.realAddress(rh)

  def sendOut[C: Writes](c: C) = out ! Json.toJson(c)

  override def postStop() = {
    super.postStop()
    pinger foreach { p => p.cancel() }
  }
}

object JsonActor {
  private val log = Logger(getClass)

  def props(ctx: ActorMeta) = Props(new JsonActor(ctx))
}

class ReplayMediator(bufferSize: Int) extends Mediator {
  val broadcastHistory = BoundedList.empty[JsValue](bufferSize)

  override def onJoined(ref: ActorRef): Unit = {
    broadcastHistory foreach { json => ref ! json }
  }

  override def onBroadcast(message: JsValue): Unit = {
    broadcastHistory += message
  }
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
class Mediator extends Actor {
  var clients: Set[ActorRef] = Set.empty

  override def receive: Receive = {
    case Broadcast(message) =>
      clients foreach { out => out ! message }
      onBroadcast(message)
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

  def onBroadcast(message: JsValue): Unit = ()

  def onClientMessage(message: JsValue, rh: RequestHeader): Unit = ()

  def onJoined(ref: ActorRef): Unit = ()

  def onLeft(ref: ActorRef): Unit = ()
}

object Mediator {
  def props() = Props(new Mediator)

  sealed trait Message

  case class ClientJoined(ref: ActorRef) extends Message

  case class ClientLeft(ref: ActorRef) extends Message

  case class Broadcast(json: JsValue) extends Message

  case class ClientMessage(json: JsValue, rh: RequestHeader) extends Message

}
