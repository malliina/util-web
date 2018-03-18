package com.malliina.play.auth

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object Execution {
  implicit val cached: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
}
