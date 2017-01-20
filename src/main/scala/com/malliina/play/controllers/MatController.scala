package com.malliina.play.controllers

import akka.stream.Materializer

trait MatController {
  implicit def mat: Materializer
}
