package com.malliina.play

import com.malliina.play.auth.Execution.cached

import scala.concurrent.Future

package object auth {

  // TODO decide what to do with this
  implicit class FutureEitherOps[L, R](fe: Future[Either[L, R]]) {
    def mapRight[LL >: L, S](code: R => Either[LL, S]): Future[Either[LL, S]] =
      fe.map(e => e.flatMap(r => code(r)))

    def mapR[S](code: R => S): Future[Either[L, S]] =
      fe.map(e => e.map(r => code(r)))

    def flatMapRight[LL >: L, S](code: R => Future[Either[LL, S]]): Future[Either[LL, S]] =
      fe.flatMap(e => e.fold(l => Future.successful(Left(l)), r => code(r)))

    def onFail[S >: R](code: L => S): Future[S] =
      fe.map(e => e.fold(l => code(l), identity))
  }

}
