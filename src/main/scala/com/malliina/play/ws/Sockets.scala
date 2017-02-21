package com.malliina.play.ws

import akka.actor.{ActorRef, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import com.malliina.play.ActorContext
import com.malliina.play.auth.{AuthFailure, Authenticator}
import com.malliina.play.ws.Sockets.{DefaultActorBufferSize, DefaultOverflowStrategy, log}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{RequestHeader, Result, Results, WebSocket}

object Sockets {
  private val log = Logger(getClass)

  val DefaultActorBufferSize = 1000
  val DefaultOverflowStrategy = OverflowStrategy.fail
}

abstract class Sockets[User](auth: Authenticator[User],
                             ctx: ActorContext,
                             actorBufferSize: Int = DefaultActorBufferSize,
                             overflowStrategy: OverflowStrategy = DefaultOverflowStrategy) {
  implicit val actorSystem = ctx.actorSystem
  implicit val mat = ctx.materializer
  implicit val ec = ctx.executionContext

  /** Builds actor Props of an authenticated client.
    *
    * @param out  outgoing messages
    * @param user the authenticated user
    * @param rh   the request headers
    * @return Props of an actor that receives incoming messages
    */
  def props(out: ActorRef, user: User, rh: RequestHeader): Props

  def onUnauthorized(rh: RequestHeader, failure: AuthFailure): Result = {
    log warn s"Unauthorized request $rh"
    Results.Unauthorized
  }

  def newSocket = WebSocket.acceptOrResult[JsValue, JsValue] { rh =>
    auth.authenticate(rh) map { authResult =>
      authResult.fold(
        failure => Left(onUnauthorized(rh, failure)),
        user => Right(actorFlow(user, rh))
      )
    }
  }

  def actorFlow(user: User, rh: RequestHeader): Flow[JsValue, JsValue, _] =
    ActorFlow.actorRef[JsValue, JsValue](out => props(out, user, rh), actorBufferSize, overflowStrategy)
}
