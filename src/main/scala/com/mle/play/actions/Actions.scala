package com.mle.play.actions

import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import com.mle.play.concurrent.ExecutionContexts

/**
 *
 * @author mle
 */
trait Actions {

  /**
   * Executes the work on a large thread pool suitable for synchronous IO.
   *
   * @return
   */
  object SyncAction extends ActionBuilder[Request] {
    protected def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]): Future[SimpleResult] = {
      block(request)
    }

    override protected def executionContext = ExecutionContexts.synchronousIO
  }
}

object Actions extends Actions
