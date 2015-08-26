package com.mle.play.concurrent

import play.api.Play.current
import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext

/**
 * @author Michael
 */
object ExecutionContexts {
  // see src/main/resources/reference.conf
  implicit val synchronousIO: ExecutionContext =
    Akka.system.dispatchers.lookup("play.akka.actor.synchronous-io")
}
