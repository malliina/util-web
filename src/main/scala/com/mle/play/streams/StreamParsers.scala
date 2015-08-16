package com.mle.play.streams

import java.io._

import com.mle.util.Log
import play.api.libs.iteratee._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{BodyParser, MultipartFormData}
import play.core.parsers.Multipart
import play.core.parsers.Multipart.PartHandler

import scala.concurrent.ExecutionContext
import com.mle.storage.{StorageSize, StorageInt}

/**
 *
 * @author mle
 */
trait StreamParsers extends Log {
  def defaultMaxLength: StorageSize = 10.gigs

  /**
   * Pushes the bytes to the supplied channel as they are received.
   *
   * @param dest channel to push to
   */
  def multiPartChannelStreaming(dest: Concurrent.Channel[Array[Byte]], maxLength: StorageSize = defaultMaxLength)(implicit ec: ExecutionContext): BodyParser[MultipartFormData[Long]] =
    multiPartByteStreaming(bytes => dest push bytes, maxLength)

  def multiPartByteStreaming(f: Array[Byte] => Unit, maxLength: StorageSize = defaultMaxLength)(implicit ec: ExecutionContext): BodyParser[MultipartFormData[Long]] =
    parse.multipartFormData(byteArrayPartConsumer(f), maxLength.toBytes)

  /**
   * Parses a multipart form-data upload in such a way that any parsed bytes are made available
   * to the returned [[InputStream]].
   *
   * @return
   */
  def multiPartStreamPiping(maxLength: StorageSize = defaultMaxLength)(implicit ec: ExecutionContext): (InputStream, BodyParser[MultipartFormData[Long]]) = {
    val (inStream, iteratee) = Streams.joinedStream()
    val parser = multiPartBodyParser(iteratee, maxLength)
    (inStream, parser)
  }

  def multiPartBodyParser[T](iteratee: Iteratee[Array[Byte], T], maxLength: StorageSize = defaultMaxLength): BodyParser[MultipartFormData[T]] =
    parse.multipartFormData(byteArrayPartHandler(iteratee), maxLength.toBytes)

  /**
   * Builds a part handler that applies the supplied function
   * to the array of bytes as they are received.
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
}

object StreamParsers extends StreamParsers
