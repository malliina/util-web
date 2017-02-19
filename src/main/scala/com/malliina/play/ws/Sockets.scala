package com.malliina.play.ws

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.Flow
import akka.stream.{Materializer, OverflowStrategy}
import com.malliina.play.auth.AuthFailure
import com.malliina.play.ws.Sockets.{DefaultActorBufferSize, DefaultOverflowStrategy}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{RequestHeader, Result, WebSocket}

import scala.concurrent.Future

object Sockets {
  private val log = Logger(getClass)

  val DefaultActorBufferSize = 1000
  val DefaultOverflowStrategy = OverflowStrategy.dropHead
}

abstract class Sockets[User](actorSystem: ActorSystem,
                             materializer: Materializer,
                             actorBufferSize: Int = DefaultActorBufferSize,
                             overflowStrategy: OverflowStrategy = DefaultOverflowStrategy) {
  implicit val as = actorSystem
  implicit val mat = materializer
  implicit val ec = mat.executionContext

  /** Builds actor Props of an authenticated client.
    *
    * @param out  outgoing messages
    * @param user the authenticated user
    * @param rh   the request headers
    * @return Props of an actor that receives incoming messages
    */
  def props(out: ActorRef, user: User, rh: RequestHeader): Props

  def authenticate(rh: RequestHeader): Future[Either[AuthFailure, User]]

  def onUnauthorized(rh: RequestHeader, failure: AuthFailure): Result

  def newSocket = WebSocket.acceptOrResult[JsValue, JsValue] { rh =>
    authenticate(rh) map { authResult =>
      authResult.fold(
        failure => Left(onUnauthorized(rh, failure)),
        user => Right(actorFlow(user, rh))
      )
    }
  }

  def actorFlow(user: User, rh: RequestHeader): Flow[JsValue, JsValue, _] =
    ActorFlow.actorRef[JsValue, JsValue](out => props(out, user, rh), actorBufferSize, overflowStrategy)
}
