package tests

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.FunSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}

class PlayTests extends FunSuite {
  test("can run test") {
    assert(1 + 1 === 2)
  }

  test("stream") {
    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()

    val expected = 42
    val completion = Promise[Int]()
    val source = Source(1 to 10)
    val sinkWithCleanup = Sink.onComplete[Int](_ => completion.trySuccess(expected))
    source.runWith(sinkWithCleanup)
    val answer = Await.result(completion.future, 10.seconds)
    assert(answer === expected)
  }
}
