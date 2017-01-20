package tests

import com.malliina.play.app.WithComponents
import org.scalatest.{FunSuite, Suite, TestData}
import org.scalatestplus.play.{OneAppPerSuite, OneAppPerTest, OneServerPerSuite, OneServerPerTest}
import play.api.ApplicationLoader.Context
import play.api.{Application, BuiltInComponents}

class AppSuite[T <: BuiltInComponents](build: Context => T)
  extends FunSuite
    with OneAppPerSuite2[T] {
  override def createComponents(context: Context): T = build(context)
}

// Similar to https://github.com/gmethvin/play-2.5.x-scala-compile-di-with-tests/blob/master/test/ScalaTestWithComponents.scala

trait WithTestComponents[T <: BuiltInComponents] extends WithComponents[T] {
  lazy val components: T = createComponents(TestAppLoader.createTestAppContext)
}

trait OneAppPerSuite2[T <: BuiltInComponents] extends OneAppPerSuite with WithTestComponents[T] {
  self: Suite =>

  override implicit lazy val app: Application = components.application
}

trait OneAppPerTest2[T <: BuiltInComponents] extends OneAppPerTest with WithTestComponents[T] {
  self: Suite =>

  override def newAppForTest(testData: TestData): Application = components.application
}

trait OneServerPerSuite2[T <: BuiltInComponents] extends OneServerPerSuite with WithTestComponents[T] {
  self: Suite =>

  override implicit lazy val app: Application = components.application
}

trait OneServerPerTest2[T <: BuiltInComponents] extends OneServerPerTest with WithTestComponents[T] {
  self: Suite =>

  override def newAppForTest(testData: TestData): Application = components.application
}
