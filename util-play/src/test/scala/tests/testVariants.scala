package tests

import munit.{FunSuite, Suite}
import play.api.ApplicationLoader.Context
import play.api.test.{DefaultTestServerFactory, RunningServer}
import play.api.{BuiltInComponents, Play}

abstract class AppSuite[T <: BuiltInComponents](build: Context => T)
  extends FunSuite
  with AppPerSuite[T] {
  override def components(context: Context): T = build(context)
}

abstract class ServerSuite[T <: BuiltInComponents](build: Context => T)
  extends FunSuite
  with ServerPerSuite[T] {
  override def components(context: Context): T = build(context)
}

trait PlayAppFixture[T <: BuiltInComponents] { self: FunSuite =>
  def components(context: Context): T
  val app = FunFixture[T](
    opts => {
      val comps = components(TestAppLoader.createTestAppContext)
      Play.start(comps.application)
      comps
    },
    comps => {
      Play.stop(comps.application)
    }
  )
}

trait PlayServerFixture[T <: BuiltInComponents] { self: FunSuite =>
  def components(context: Context): T
  val server = FunFixture[RunningServer](
    opts => {
      val comps = components(TestAppLoader.createTestAppContext)
      DefaultTestServerFactory.start(comps.application)
    },
    running => {
      running.stopServer.close()
    }
  )
}

trait AppPerSuite[T <: BuiltInComponents] { self: Suite =>
  def components(context: Context): T
  val testApp: Fixture[T] = new Fixture[T]("test-app") {
    private var comps: Option[T] = None
    def apply() = comps.get
    override def beforeAll(): Unit = {
      val c = components(TestAppLoader.createTestAppContext)
      comps = Option(c)
      Play.start(c.application)
    }
    override def afterAll(): Unit = {
      comps.foreach(c => Play.stop(c.application))
    }
  }

  override def munitFixtures = Seq(testApp)
}

trait ServerPerSuite[T <: BuiltInComponents] { self: Suite =>
  def components(context: Context): T
  val testServer: Fixture[RunningServer] = new Fixture[RunningServer]("test-server") {
    private var runningServer: RunningServer = null
    def apply() = runningServer
    override def beforeAll(): Unit = {
      val app = components(TestAppLoader.createTestAppContext)
      runningServer = DefaultTestServerFactory.start(app.application)
    }
    override def afterAll(): Unit = {
      runningServer.stopServer.close()
    }
  }
  def port = testServer().endpoints.httpEndpoint.map(_.port).get

  override def munitFixtures = Seq(testServer)
}
