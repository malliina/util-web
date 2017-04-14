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

case class ActorInfo(out: ActorRef, rh: RequestHeader)
  extends ActorMeta

trait ActorMeta {
  def out: ActorRef

  def rh: RequestHeader
}

case class DefaultActorConfig[U](out: ActorRef, rh: RequestHeader, user: U)
  extends ActorConfig[U]

trait ActorConfig[U] extends ActorMeta {
  def user: U
}

class ObserverActor(events: Observable[JsValue], ctx: ActorMeta)
  extends JsonActor(ctx) {
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
