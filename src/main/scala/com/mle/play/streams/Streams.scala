package com.mle.play.streams

import java.io._
import play.api.libs.iteratee.{Done, Input, Cont, Iteratee}
import java.nio.file.Path

/**
 *
 * @author mle
 */
trait Streams {
  /**
   * http://stackoverflow.com/questions/12066993/uploading-file-as-stream-in-play-framework-2-0
   *
   * @return an [[InputStream]] and an [[Iteratee]] such that any bytes consumed by the Iteratee are made available to the InputStream
   */
  def joinedStream(): (InputStream, Iteratee[Array[Byte], OutputStream]) = {
    val outStream = new PipedOutputStream()
    val inStream = new PipedInputStream(outStream)
    val iteratee = fromOutputStream(outStream).map(os => {
      os.close()
      os
    })
    (inStream, iteratee)
  }

  /**
   * @return an [[Iteratee]] that writes any consumed bytes to `os`
   */
  def fromOutputStream(os: OutputStream) =
    Iteratee.fold[Array[Byte], OutputStream](os)((state, data) => {
      state.write(data)
      state
    })

  /**
   * @param file destination file
   * @return an [[Iteratee]] that writes bytes to `file`, keeping track of the number of bytes written
   */
  def fileWriter(file: Path): Iteratee[Array[Byte], Long] = {
    def fromStreamAcc(stream: OutputStream, bytesWritten: Long): Iteratee[Array[Byte], Long] = Cont {
      case e@Input.EOF =>
        stream.close()
        Done(bytesWritten, e)
      case Input.El(data) =>
        stream.write(data)
        fromStreamAcc(stream, bytesWritten + data.length)
      case Input.Empty =>
        fromStreamAcc(stream, bytesWritten)
    }
    val outputStream = new BufferedOutputStream(new FileOutputStream(file.toFile))
    fromStreamAcc(outputStream, 0)
  }
}

object Streams extends Streams
