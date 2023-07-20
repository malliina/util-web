package com.malliina.database

import cats.Monad
import cats.effect.kernel.Resource
import cats.effect.{Async, Sync}
import cats.syntax.flatMap.toFlatMapOps
import com.malliina.util.AppLogger
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.log.{ExecFailure, LogEvent, ProcessingFailure, Success}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

import scala.concurrent.duration.DurationInt

object DoobieDatabase:
  private val log = AppLogger(getClass)

  def init[F[_]: Async](conf: Conf): Resource[F, DoobieDatabase[F]] =
    if conf.autoMigrate then withMigrations(conf) else default(conf)

  def default[F[_]: Async](conf: Conf): Resource[F, DoobieDatabase[F]] =
    for tx <- transactor(config(conf))
    yield DoobieDatabase(tx)

  private def withMigrations[F[_]: Async](conf: Conf): Resource[F, DoobieDatabase[F]] =
    Resource.eval(migrate(conf)).flatMap { _ => default(conf) }

  private def migrate[F[_]: Sync](conf: Conf): F[MigrateResult] = Sync[F].delay {
    val flyway = Flyway.configure.dataSource(conf.url, conf.user, conf.pass).load()
    flyway.migrate()
  }

  private def config(conf: Conf): HikariConfig =
    val hikari = new HikariConfig()
    hikari.setDriverClassName(Conf.MySQLDriver)
    hikari.setJdbcUrl(conf.url)
    hikari.setUsername(conf.user)
    hikari.setPassword(conf.pass)
    hikari.setMaxLifetime(60.seconds.toMillis)
    hikari.setMaximumPoolSize(conf.maxPoolSize)
    hikari

  private def transactor[F[_]: Async](conf: HikariConfig): Resource[F, DataSourceTransactor[F]] =
    val F = Async[F]
    val syncLogHandler: LogEvent => Unit =
      case Success(sql, args, _, exec, processing) =>
        val logger: String => Unit = if processing > 1.seconds then log.info else log.debug
        logger(s"OK '$sql' exec ${exec.toMillis} ms processing ${processing.toMillis} ms.")
      case ProcessingFailure(sql, args, _, exec, processing, failure) =>
        log.error(s"Failed '$sql' in ${exec + processing}.", failure)
      case ExecFailure(sql, args, _, exec, failure) =>
        log.error(s"Exec failed '$sql' in $exec.'", failure)
    val logHandler: LogHandler[F] = event => F.delay(syncLogHandler(event))
    val connect = F.delay {
      log.info(
        s"Connecting to '${conf.getJdbcUrl}' with pool size ${conf.getMaximumPoolSize} as ${conf.getUsername}..."
      )
      new HikariDataSource(conf)
    }
    val disconnect = (ds: HikariDataSource) =>
      F.delay {
        log.info(s"Disconnecting from ${ds.getJdbcUrl} as ${conf.getUsername}...")
        ds.close()
      }
    for
      ec <- ExecutionContexts.fixedThreadPool[F](16) // connect EC
      ds <- Resource.make(connect)(disconnect)
    yield Transactor.fromDataSource[F](ds, ec, Option(logHandler))

class DoobieDatabase[F[_]: Async](tx: DataSourceTransactor[F]):
  def run[T](io: ConnectionIO[T]): F[T] = io.transact(tx)
