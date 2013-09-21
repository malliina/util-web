package com.mle.play.concurrent

import concurrent.ExecutionContext
import play.api.libs.concurrent.Akka
import play.api.Play.current

/**
 * @author Michael
 */
object ExecutionContexts {
  // see src/main/resources/reference.conf
  implicit val synchronousIO: ExecutionContext =
    Akka.system.dispatchers.lookup("play.akka.actor.synchronous-io")
}
