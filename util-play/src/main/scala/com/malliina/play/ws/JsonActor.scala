package com.malliina.play.ws

import akka.actor.{Actor, Cancellable, Props}
import com.malliina.play.http.Proxies
import com.malliina.play.json.JsonMessages
import com.malliina.play.ws.JsonActor.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}

import scala.concurrent.duration.DurationInt

class JsonActor(ctx: ActorMeta) extends Actor {
  implicit val ec = context.dispatcher
  val out = ctx.out
  val rh = ctx.rh
  var pinger: Option[Cancellable] = None

  override def preStart() = {
    super.preStart()
    scheduleHealthCheck()
  }

  def scheduleHealthCheck(): Unit = {
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
    log info s"Client '$address' says '$message'."

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
