package com.malliina.play.streams

import java.io._

import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.malliina.storage.StorageSize
import play.api.http.LazyHttpErrorHandler
import play.api.http.Status._
import play.api.libs.iteratee._
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData._
import play.api.mvc.{BodyParser, MultipartFormData, RequestHeader, Result}
import play.core.parsers.Multipart
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}

import scala.concurrent.{ExecutionContext, Future}

trait StreamParsers {
  /** Pushes the bytes to the supplied channel as they are received.
    *
    * @param dest channel to push to
    */
  def multiPartChannelStreaming(dest: SourceQueue[ByteString], maxLength: StorageSize)(implicit mat: Materializer): BodyParser[MultipartFormData[Long]] =
    multiPartByteStreaming(bytes => (dest offer bytes).map(_ => ())(mat.executionContext), maxLength)

  def multiPartByteStreaming(f: ByteString => Future[Unit], maxLength: StorageSize)(implicit mat: Materializer): BodyParser[MultipartFormData[Long]] = {
    //    multipartFormDataFixed(byteArrayPartConsumer(f), maxLength)
    Multipart.multipartParser(maxLength.toBytes.toInt, byteArrayPartConsumer(f))
  }

  /** Parses a multipart form-data upload in such a way that any parsed bytes are made available to the returned [[InputStream]].
    *
    * @return
    */
  def multiPartStreamPiping(maxLength: StorageSize)(implicit mat: Materializer): (InputStream, BodyParser[MultipartFormData[Long]]) = {
    val (inStream, iteratee) = Streams.joinedStream()(mat.executionContext)
    val parser = multiPartBodyParser(iteratee, maxLength)
    (inStream, parser)
  }

  def multiPartBodyParser[T](sink: Sink[ByteString, Future[T]], maxLength: StorageSize)(implicit mat: Materializer): BodyParser[MultipartFormData[T]] = {
    Multipart.multipartParser(maxLength.toBytes.toInt, byteArrayPartHandler(sink)(mat.executionContext))
    //    multipartFormDataFixed(byteArrayPartHandler(sink), maxLength)
  }


  /** Builds a part handler that applies the supplied function to the array of bytes as they are received.
    *
    * @param f what to do with the bytes
    * @return a part handler memorizing the total number of bytes consumed
    */
  protected def byteArrayPartConsumer(f: ByteString => Future[Unit])(implicit mat: Materializer): FilePartHandler[Long] = {
    val byteCalculator: Sink[ByteString, Future[Long]] = Sink.fold[Long, ByteString](0)((acc, bytes) => acc + bytes.length)
    val asyncSink = Flow[ByteString].mapAsync(1)(bytes => f(bytes).map(_ => bytes)(mat.executionContext)).toMat(byteCalculator)(Keep.right)
    byteArrayPartHandler(asyncSink)(mat.executionContext)

    //    Flow[ByteString].mapAsync(4)(bytes => )
    //    val sink: Sink[ByteString, Future[Long]] = Sink.fold[Long, ByteString](0) { (count, bytes) =>
    //      f(bytes)
    //      count + bytes.length
    //    }
    //    byteArrayPartHandler(sink)
  }

  //  protected def byteArrayPartConsumer(f: Array[Byte] => Unit)(implicit ec: ExecutionContext): PartHandler[FilePart[Long]] = {
  //    val iteratee = Iteratee.fold[Array[Byte], Long](0)((count, bytes) => {
  //      //      log debug s"Bytes handled: $count"
  //      f(bytes)
  //      count + bytes.length
  //    })
  //    byteArrayPartHandler(iteratee)
  //  }

  /** Builds a part handler that uses the supplied sink to handle the bytes as they are received.
    *
    * @param sink input byte handler
    * @tparam T eventual value produced by iteratee
    * @return
    */
  protected def byteArrayPartHandler[T](sink: Sink[ByteString, Future[T]])(implicit ec: ExecutionContext): Multipart.FilePartHandler[T] = {
    //    byteArrayPartHandler2(Accumulator(sink))
    handleFilePart(fi => Accumulator(sink))
  }

  protected def byteArrayPartHandler2[T](acc: Accumulator[ByteString, FilePart[T]]): Multipart.FilePartHandler[T] = {
    case FileInfo(partName, filename, contentType) =>
      acc
  }

  /** Taken from Multipart.scala.
    *
    * @param handler
    * @tparam A
    * @return
    */
  protected def handleFilePart[A](handler: FileInfo => Accumulator[ByteString, A])(implicit ec: ExecutionContext): FilePartHandler[A] = (fi: FileInfo) => {
    val safeFileName = fi.fileName.split('\\').takeRight(1).mkString
    val partName = fi.partName
    val contentType = fi.contentType
    handler(FileInfo(partName, safeFileName, contentType))
      .map(a => FilePart(partName, safeFileName, contentType, a))
  }

  //  def byteArrayPartHandler[T](in: Iteratee[Array[Byte], T]): PartHandler[FilePart[T]] = {
  //    Multipart.handleFilePart {
  //      case Multipart.FileInfo(partName, fileName, contentType) =>
  //        in
  //    }
  //  }

  /** Copied from play.api.mvc.ContentTypes.scala for now
    */
  private def checkForEof[A](request: RequestHeader): A => Iteratee[Array[Byte], Either[Result, A]] = { eofValue: A =>
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    def cont: Iteratee[Array[Byte], Either[Result, A]] = Cont {
      case in@Input.El(e) =>
        val badResult: Future[Result] = createBadResult("Request Entity Too Large", REQUEST_ENTITY_TOO_LARGE)(request)
        Iteratee.flatten(badResult.map(r => Done(Left(r), in)))
      case in@Input.EOF =>
        Done(Right(eofValue), in)
      case Input.Empty =>
        cont
    }
    cont
  }

  /** Copied from play.api.mvc.ContentTypes.scala for now
    */
  private def createBadResult(msg: String, statusCode: Int = BAD_REQUEST): RequestHeader => Future[Result] = { request =>
    LazyHttpErrorHandler.onClientError(request, statusCode, msg)
  }
}

object StreamParsers extends StreamParsers {
  //  private val log = Logger(getClass)
}
