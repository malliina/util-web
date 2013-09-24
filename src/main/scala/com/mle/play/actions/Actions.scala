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

  /**
   * Implement <code>map</code>.
   *
   * @tparam R type of mapped request
   */
  abstract class MappingActionBuilder[R[C]] extends ActionBuilder[R] {
    protected def invokeBlock[A](request: Request[A], block: (R[A]) => Future[SimpleResult]): Future[SimpleResult] =
      map(request).map(req => onSuccess(req, block)).getOrElse(Future.successful(onFailure(request)))

    protected def map[A](request: Request[A]): Option[R[A]]

    protected def onSuccess[A](request: R[A], f: (R[A]) => Future[SimpleResult]): Future[SimpleResult] =
      f(request)

    protected def onFailure[A](request: Request[A]): SimpleResult =
      Results.BadRequest
  }

//  /**
//   * Enforces authentication for an action.
//   *
//   * @param action action to do if authentication succeeds
//   * @tparam A type of request content
//   * @tparam U type of user
//   */
//  abstract class AuthChecked[A, U](action: Action[A]) extends DefaultAction[A](action) {
//    override def apply(request: Request[A]): Future[SimpleResult] =
//      auth(request)
//        .map(user => onAuthenticated(new AuthenticatedRequest(user, request)))
//        .getOrElse(Future.successful(onAuthFail(request)))
//
//    def auth(request: Request[A]): Option[U]
//
//    def onAuthenticated(request: AuthenticatedRequest[A, U]): Future[SimpleResult] =
//      action(request)
//
//    def onAuthFail(request: Request[A]): SimpleResult
//  }

  abstract class DefaultAction[A](action: Action[A]) extends Action[A] {
    def parser: BodyParser[A] = action.parser
  }

}

object Actions extends Actions
