package com.mle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author mle
 */
package object concurrent2 {

  implicit class FutureOps2[T](f: Future[T]) {
    def checkOrElse[U >: T](check: T => Boolean, orElse: => Future[U]): Future[U] = {
      f.flatMap(t => if (check(t)) Future.successful(t) else orElse)
    }
  }

}
