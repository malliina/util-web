package com.malliina.play.actions

import akka.actor.ActorSystem
import com.malliina.play.concurrent.ExecutionContexts
import play.api.mvc._

import scala.concurrent.Future

trait Actions {

  /**
    * Executes the work on a large thread pool suitable for synchronous IO.
    */
  class SyncAction(actorSystem: ActorSystem) extends DefaultActionBuilder {
    override protected val executionContext = new ExecutionContexts(actorSystem).synchronousIO
  }

  /**
    * Default action builder, override what you need.
    */
  abstract class DefaultActionBuilder extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] =
      block(request)
  }

}

object Actions extends Actions
