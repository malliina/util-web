package com.mle.play.actions

import play.api.mvc._
import scala.concurrent.Future
import com.mle.play.concurrent.ExecutionContexts
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.SimpleResult

/**
 *
 * @author mle
 */
trait Actions {

  /**
   * Executes the work on a large thread pool suitable for synchronous IO.
   */
  class SyncAction extends DefaultActionBuilder {
    override protected val executionContext = ExecutionContexts.synchronousIO
  }

  object SyncAction extends SyncAction

  /**
   * Default action builder, override what you need.
   */
  abstract class DefaultActionBuilder extends ActionBuilder[Request] {
    protected def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]): Future[SimpleResult] =
      block(request)
  }
}

object Actions extends Actions
