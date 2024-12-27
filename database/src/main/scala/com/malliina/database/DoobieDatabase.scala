package com.malliina.database

import cats.effect.kernel.Resource
import cats.effect.{Async, Sync}
import cats.implicits.toFunctorOps
import com.malliina.util.AppLogger
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.log.{ExecFailure, LogEvent, ProcessingFailure, Success}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import javax.sql.DataSource
import scala.concurrent.duration.DurationInt

object DoobieDatabase:
  private val log = AppLogger(getClass)

  def init[F[_]: Async](conf: Conf): Resource[F, DoobieDatabase[F]] =
    init(conf, makeLogHandler[F])

  def init[F[_]: Async](conf: Conf, logHandler: LogHandler[F]): Resource[F, DoobieDatabase[F]] =
    if conf.autoMigrate then withMigrations(conf) else default(conf)

  def default[F[_]: Async](conf: Conf): Resource[F, DoobieDatabase[F]] =
    default[F](conf, makeLogHandler[F])

  def default[F[_]: Async](conf: Conf, logHandler: LogHandler[F]): Resource[F, DoobieDatabase[F]] =
    pooledDatabase[F](poolConfig(conf))

  def fast[F[_]: Async](conf: Conf): F[DoobieDatabase[F]] =
    val maybeMigration =
      if conf.autoMigrate then migrate[F](conf).map(r => Option(r)) else Sync[F].pure(None)
    maybeMigration.map(_ => DoobieDatabase(noPoolTransactor(conf)))

  def dataSource[F[_]: Async](ds: DataSource): Resource[F, DoobieDatabase[F]] =
    ExecutionContexts
      .fixedThreadPool[F](16) // connect EC
      .map: ec =>
        val tx = Transactor.fromDataSource[F](ds, ec, Option(makeLogHandler[F]))
        DoobieDatabase(tx)

  private def withMigrations[F[_]: Async](conf: Conf): Resource[F, DoobieDatabase[F]] =
    Resource.eval(migrate(conf)).flatMap(_ => default(conf))

  private def migrate[F[_]: Sync](conf: Conf): F[MigrateResult] = Sync[F].delay:
    val flyway = Flyway.configure
      .dataSource(conf.url.url, conf.user, conf.pass.pass)
      .table(conf.schemaTable)
      .load()
    flyway.migrate()

  private def poolConfig(conf: Conf): HikariConfig =
    val hikari = new HikariConfig()
    hikari.setDriverClassName(conf.driver)
    hikari.setJdbcUrl(conf.url.url)
    hikari.setUsername(conf.user)
    hikari.setPassword(conf.pass.pass)
    hikari.setMaxLifetime(60.seconds.toMillis)
    hikari.setMaximumPoolSize(conf.maxPoolSize)
    hikari

  private def makeLogHandler[F[_]: Async]: LogHandler[F] =
    val syncLogHandler: LogEvent => Unit =
      case Success(sql, args, _, exec, processing) =>
        val logger: String => Unit = if processing > 1.seconds then log.info else log.debug
        logger(s"OK '$sql' exec ${exec.toMillis} ms processing ${processing.toMillis} ms.")
      case ProcessingFailure(sql, args, _, exec, processing, failure) =>
        log.error(s"Failed '$sql' in ${exec + processing}.", failure)
      case ExecFailure(sql, args, _, exec, failure) =>
        log.error(s"Exec failed '$sql' in $exec.'", failure)
    event => Async[F].delay(syncLogHandler(event))

  private def pooledDatabase[F[_]: Async](conf: HikariConfig): Resource[F, DoobieDatabase[F]] =
    val F = Async[F]
    val connect = F.delay:
      log.info(
        s"Connecting to '${conf.getJdbcUrl}' with pool size ${conf.getMaximumPoolSize} as ${conf.getUsername}..."
      )
      new HikariDataSource(conf)
    val disconnect = (ds: HikariDataSource) =>
      F.delay:
        log.info(s"Disconnecting from ${ds.getJdbcUrl} as ${conf.getUsername}...")
        ds.close()

    for
      ds <- Resource.make(connect)(disconnect)
      db <- dataSource(ds)
    yield db

  private def noPoolTransactor[F[_]: Async](conf: Conf): Transactor[F] =
    Transactor.fromDriverManager[F](
      conf.driver,
      conf.url.url,
      user = conf.user,
      password = conf.pass.pass,
      logHandler = Option(makeLogHandler[F])
    )

class DoobieDatabase[F[_]: Async](tx: Transactor[F]):
  def run[T](io: ConnectionIO[T]): F[T] = io.transact(tx)
