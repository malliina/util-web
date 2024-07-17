package com.malliina.http4s

import com.malliina.http.{Errors, SingleError}
import com.malliina.values.ErrorMessage
import org.http4s.{ParseFailure, Query, QueryParamDecoder, QueryParameterValue}

object QueryParsers extends QueryParsers

trait QueryParsers:
  def parseOrDefault[T: QueryParamDecoder](
    q: Query,
    key: String,
    default: => T
  ): Either[Errors, T] =
    parseOpt[T](q, key).getOrElse(Right(default))

  def parse[T: QueryParamDecoder](q: Query, key: String): Either[Errors, T] =
    parseOpt[T](q, key)
      .getOrElse(Left(Errors(s"Query key not found: '$key'.")))

  def parseOpt[T: QueryParamDecoder](q: Query, key: String): Option[Either[Errors, T]] =
    q.params.get(key).map(g => parseValue[T](g))

  def parseOptE[T: QueryParamDecoder](q: Query, key: String): Either[Errors, Option[T]] =
    parseOpt(q, key).map(e => e.map(t => Option(t))).getOrElse(Right(None))

  def decoder[T](validate: String => Either[ErrorMessage, T]): QueryParamDecoder[T] =
    QueryParamDecoder.stringQueryParamDecoder.emap: s =>
      validate(s).left.map(err => parseFailure(err.message))

  def list[T: QueryParamDecoder](key: String, q: Query): Either[Errors, Seq[T]] =
    q.multiParams
      .get(key)
      .map: list =>
        val results = list.map(s => parseValue[T](s))
        collect(results)
      .getOrElse(Right(Nil))

  private def parseValue[T](s: String)(using dec: QueryParamDecoder[T]) =
    dec
      .decode(QueryParameterValue(s))
      .toEither
      .left
      .map: failures =>
        Errors(failures.map(pf => SingleError.input(pf.sanitized)))

  def collect[L, R](results: Seq[Either[L, R]]): Either[L, Seq[R]] =
    results.foldLeft[Either[L, Seq[R]]](Right(Seq.empty[R])): (acc, elem) =>
      elem.flatMap(ok => acc.map(rs => rs :+ ok))

  private def parseFailure(message: String) = ParseFailure(message, message)
