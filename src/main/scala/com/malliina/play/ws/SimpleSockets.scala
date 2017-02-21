package com.malliina.play.ws

import akka.actor.{ActorRef, Props}
import com.malliina.play.ActorContext
import com.malliina.play.auth.Authenticator
import play.api.mvc.RequestHeader

/** Manages websockets.
  *
  * To send a message to all connected clients:
  * `mediator ! Broadcast(myJsonMessage)`
  *
  * The actor created from `mediatorProps` will receive any
  * messages sent from connected websockets.
  */
class SimpleSockets[User](mediatorProps: Props,
                          auth: Authenticator[User],
                          ctx: ActorContext)
  extends Sockets[User](auth, ctx) {
  val mediator = actorSystem.actorOf(mediatorProps)

  /** Builds actor Props of an authenticated client.
    *
    * @param out  outgoing messages
    * @param user the authenticated user
    * @param rh   the request headers
    * @return Props of an actor that receives incoming messages
    */
  override def props(out: ActorRef, user: User, rh: RequestHeader): Props =
    ClientActor.props(SimpleClientContext(out, rh, mediator))
}
