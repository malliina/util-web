package com.malliina.play.concurrent

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

class ExecutionContexts(actorSystem: ActorSystem) {
  // see src/main/resources/reference.conf
  implicit val synchronousIO: ExecutionContext =
    actorSystem.dispatchers.lookup("play.akka.actor.synchronous-io")
}
