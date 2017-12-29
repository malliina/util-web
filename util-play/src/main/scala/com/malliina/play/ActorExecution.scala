package com.malliina.play

import akka.actor.ActorSystem
import akka.stream.Materializer

case class ActorExecution(actorSystem: ActorSystem, materializer: Materializer) {
  val executionContext = materializer.executionContext
}
