package com.mle.play.concurrent

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

/**
 * @author Michael
 */
class ExecutionContexts(actorSystem: ActorSystem) {
  // see src/main/resources/reference.conf
  implicit val synchronousIO: ExecutionContext =
    actorSystem.dispatchers.lookup("play.akka.actor.synchronous-io")
}
