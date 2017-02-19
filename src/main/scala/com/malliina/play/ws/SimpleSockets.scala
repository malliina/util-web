package com.malliina.play.ws

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import com.malliina.play.auth.{AuthFailure, Authenticator}
import com.malliina.play.ws.SimpleSockets.log
import play.api.Logger
import play.api.mvc.{RequestHeader, Result, Results}

class SimpleSockets[User](auth: Authenticator[User],
                          actorSystem: ActorSystem,
                          materializer: Materializer)
  extends Sockets[User](actorSystem, materializer) {
  val mediator = actorSystem.actorOf(Mediator.props())

  /** Builds actor Props of an authenticated client.
    *
    * @param out  outgoing messages
    * @param user the authenticated user
    * @param rh   the request headers
    * @return Props of an actor that receives incoming messages
    */
  override def props(out: ActorRef, user: User, rh: RequestHeader): Props =
    ClientActor.props(SimpleClientContext(out, rh, mediator))

  override def authenticate(rh: RequestHeader) = auth.authenticate(rh)

  override def onUnauthorized(rh: RequestHeader, failure: AuthFailure): Result = {
    log warn s"Unauthorized request $rh"
    Results.Unauthorized
  }
}

object SimpleSockets {
  private val log = Logger(getClass)
}
