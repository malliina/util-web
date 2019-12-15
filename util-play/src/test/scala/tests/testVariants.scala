package tests

import com.malliina.play.app.WithComponents
import org.scalatest.{FunSuite, TestSuite}
import org.scalatestplus.play._
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponents

abstract class AppSuite[T <: BuiltInComponents](build: Context => T)
  extends FunSuite
  with OneAppPerSuite2[T] {
  override def createComponents(context: Context): T = build(context)
}

abstract class ServerSuite[T <: BuiltInComponents](build: Context => T)
  extends FunSuite
  with OneServerPerSuite2[T] {
  override def createComponents(context: Context) = build(context)
}

// Similar to https://github.com/gmethvin/play-2.5.x-scala-compile-di-with-tests/blob/master/test/ScalaTestWithComponents.scala

trait WithTestComponents[T <: BuiltInComponents]
  extends WithComponents[T]
  with FakeApplicationFactory {
  lazy val components: T = createComponents(TestAppLoader.createTestAppContext)

  override def fakeApplication() = components.application
}

trait OneAppPerSuite2[T <: BuiltInComponents]
  extends BaseOneAppPerSuite
  with WithTestComponents[T] {
  self: TestSuite =>
}

trait OneAppPerTest2[T <: BuiltInComponents] extends BaseOneAppPerTest with WithTestComponents[T] {
  self: TestSuite =>
}

trait OneServerPerSuite2[T <: BuiltInComponents]
  extends BaseOneServerPerSuite
  with WithTestComponents[T] {
  self: TestSuite =>
}

trait OneServerPerTest2[T <: BuiltInComponents]
  extends BaseOneServerPerTest
  with WithTestComponents[T] {
  self: TestSuite =>
}
