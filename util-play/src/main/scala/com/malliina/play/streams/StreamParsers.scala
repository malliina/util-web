package com.malliina.play.streams

import java.io._

import com.malliina.storage.StorageSize
import play.api.http.LazyHttpErrorHandler
import play.api.http.Status._
import play.api.libs.iteratee._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{BodyParser, MultipartFormData, RequestHeader, Result}
import play.core.parsers.Multipart
import play.core.parsers.Multipart.PartHandler

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author mle
 */
trait StreamParsers {
  /**
   * Pushes the bytes to the supplied channel as they are received.
   *
   * @param dest channel to push to
   */
  def multiPartChannelStreaming(dest: Concurrent.Channel[Array[Byte]], maxLength: StorageSize)(implicit ec: ExecutionContext): BodyParser[MultipartFormData[Long]] =
    multiPartByteStreaming(bytes => dest push bytes, maxLength)

  def multiPartByteStreaming(f: Array[Byte] => Unit, maxLength: StorageSize)(implicit ec: ExecutionContext): BodyParser[MultipartFormData[Long]] =
    multipartFormDataFixed(byteArrayPartConsumer(f), maxLength)

  /**
   * Parses a multipart form-data upload in such a way that any parsed bytes are made available to the returned [[InputStream]].
   *
   * @return
   */
  def multiPartStreamPiping(maxLength: StorageSize)(implicit ec: ExecutionContext): (InputStream, BodyParser[MultipartFormData[Long]]) = {
    val (inStream, iteratee) = Streams.joinedStream()
    val parser = multiPartBodyParser(iteratee, maxLength)
    (inStream, parser)
  }

  def multiPartBodyParser[T](iteratee: Iteratee[Array[Byte], T], maxLength: StorageSize): BodyParser[MultipartFormData[T]] =
    multipartFormDataFixed(byteArrayPartHandler(iteratee), maxLength)

  /**
   * Builds a part handler that applies the supplied function to the array of bytes as they are received.
   *
   * @param f what to do with the bytes
   * @return a part handler memorizing the total number of bytes consumed
   */
  def byteArrayPartConsumer(f: Array[Byte] => Unit)(implicit ec: ExecutionContext): PartHandler[FilePart[Long]] = {
    val iteratee = Iteratee.fold[Array[Byte], Long](0)((count, bytes) => {
      //      log debug s"Bytes handled: $count"
      f(bytes)
      count + bytes.length
    })
    byteArrayPartHandler(iteratee)
  }

  /**
   * Builds a part handler that uses the supplied iteratee to handle the bytes as they are received.
   *
   * @param in input byte handler
   * @tparam T eventual value produced by iteratee
   * @return
   */
  def byteArrayPartHandler[T](in: Iteratee[Array[Byte], T]): PartHandler[FilePart[T]] = {
    Multipart.handleFilePart {
      case Multipart.FileInfo(partName, fileName, contentType) =>
        in
    }
  }

  /**
   * Adapted from play.api.mvc.ContentTypes.scala for now
   */
  def multipartFormDataFixed[A](filePartHandler: Multipart.PartHandler[FilePart[A]], maxLength: StorageSize): BodyParser[MultipartFormData[A]] = {
    val maxLengthLong = maxLength.toBytes
    BodyParser("multipartFormData") { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline

      val parser = Traversable.takeUpTo[Array[Byte]](maxLengthLong).transform(
        Multipart.multipartParser(maxLengthLong.toInt, filePartHandler)(request)
      ).flatMap {
        case d @ Left(r) => Iteratee.eofOrElse(r)(d)
        case d => checkForEof(request)(d)
      }

      parser.map {
        case Left(tooLarge) => Left(tooLarge)
        case Right(Left(badResult)) => Left(badResult)
        case Right(Right(body)) => Right(body)
      }
    }
  }

  /**
   * Copied from play.api.mvc.ContentTypes.scala for now
   */
  private def checkForEof[A](request: RequestHeader): A => Iteratee[Array[Byte], Either[Result, A]] = { eofValue: A =>
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    def cont: Iteratee[Array[Byte], Either[Result, A]] = Cont {
      case in @ Input.El(e) =>
        val badResult: Future[Result] = createBadResult("Request Entity Too Large", REQUEST_ENTITY_TOO_LARGE)(request)
        Iteratee.flatten(badResult.map(r => Done(Left(r), in)))
      case in @ Input.EOF =>
        Done(Right(eofValue), in)
      case Input.Empty =>
        cont
    }
    cont
  }

  /**
   * Copied from play.api.mvc.ContentTypes.scala for now
   */
  private def createBadResult(msg: String, statusCode: Int = BAD_REQUEST): RequestHeader => Future[Result] = { request =>
    LazyHttpErrorHandler.onClientError(request, statusCode, msg)
  }
}

object StreamParsers extends StreamParsers {
//  private val log = Logger(getClass)
}
