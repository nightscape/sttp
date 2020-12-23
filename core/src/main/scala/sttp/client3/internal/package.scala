package sttp.client3

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.{Buffer, ByteBuffer}

import scala.annotation.{implicitNotFound, tailrec}

package object internal {
  private[client3] def contentTypeWithCharset(ct: String, charset: String): String =
    s"$ct; charset=$charset"

  private[client3] def charsetFromContentType(ct: String): Option[String] =
    ct.split(";").map(_.trim.toLowerCase).collectFirst {
      case s if s.startsWith("charset=") && s.substring(8).trim != "" => s.substring(8).trim
    }

  private[client3] def transfer(is: InputStream, os: OutputStream): Unit = {
    var read = 0
    val buf = new Array[Byte](IOBufferSize)

    @tailrec
    def transfer(): Unit = {
      read = is.read(buf, 0, buf.length)
      if (read != -1) {
        os.write(buf, 0, read)
        transfer()
      }
    }

    transfer()
  }

  private[client3] def toByteArray(is: InputStream): Array[Byte] = {
    val os = new ByteArrayOutputStream
    transfer(is, os)
    os.toByteArray
  }

  private[client3] def emptyInputStream(): InputStream = new ByteArrayInputStream(Array[Byte]())

  private[client3] def concatByteBuffers(bb1: ByteBuffer, bb2: ByteBuffer): ByteBuffer = {
    val buf = ByteBuffer
      .allocate(bb1.array().length + bb2.array().length)
      .put(bb1)
      .put(bb2)
    // rewind() returns Buffer in Java8, and ByteBuffer in Java11
    // calling the method from the base class to avoid NoSuchMethodError
    (buf: Buffer).rewind()
    buf
  }

  /** Removes quotes surrounding the charset.
    */
  private[client3] def sanitizeCharset(charset: String): String = {
    val c2 = charset.trim()
    val c3 = if (c2.startsWith("\"")) c2.substring(1) else c2
    if (c3.endsWith("\"")) c3.substring(0, c3.length - 1) else c3
  }

  private[client3] val Utf8 = "utf-8"
  private[client3] val Iso88591 = "iso-8859-1"
  private[client3] val CrLf = "\r\n"

  private[client3] def throwNestedMultipartNotAllowed =
    throw new IllegalArgumentException("Nested multipart bodies are not allowed")

  private[client3] type ReplayableBody = Option[Either[Array[Byte], SttpFile]]
  private[client3] def replayableBody(a: Array[Byte]): ReplayableBody = Some(Left(a))
  private[client3] def replayableBody(f: SttpFile): ReplayableBody = Some(Right(f))
  private[client3] val nonReplayableBody: ReplayableBody = None

  private[client3] val IOBufferSize = 1024

  implicit class RichByteBuffer(byteBuffer: ByteBuffer) {
    def safeRead(): Array[Byte] = {
      if (byteBuffer.hasArray) {
        byteBuffer.array()
      } else {
        val array = new Array[Byte](byteBuffer.remaining())
        byteBuffer.get(array)
        array
      }
    }
  }
}
