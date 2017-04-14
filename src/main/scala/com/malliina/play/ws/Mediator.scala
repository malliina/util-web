package com.malliina.play.ws

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.malliina.play.ws.Mediator.{Broadcast, ClientJoined, ClientLeft, ClientMessage}
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

/** To broadcast a message to all connected clients, send a `Broadcast(message)`
  * to this actor.
  *
  * Calls `onClientMessage` when a message is received from a client.
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
