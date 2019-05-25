package com.softwaremill.sttp

import scala.collection.mutable

class ToCurlConverter[R <: RequestT[Id, _, _]] {

  def apply(request: R): String = {
    val params = List(extractOptions(_), extractMethod(_), extractHeaders(_), extractBody(_))
      .map(addSpaceIfNotEmpty)
      .reduce((acc, item) => r => acc(r) + item(r))
      .apply(request)

    s"curl$params ${request.uri}"
  }

  private def extractMethod(r: R): String = {
    s"-X ${r.method.m}"
  }

  private def extractHeaders(r: R): String = {
    r.headers
      .collect {
        case (k, v) => s"""-H "$k: $v""""
      }
      .mkString(" ")
  }

  private def extractBody(r: R): String = {
    r.body match {
      case StringBody(text, _, _) if r.headers.toMap.get(HeaderNames.ContentType).forall(_ == MediaTypes.Form) =>
        s"""-F '$text'"""
      case StringBody(text, _, _) => s"""--data '$text'"""
      case ByteArrayBody(_, _)    => s"--data-binary <PLACEHOLDER>"
      case ByteBufferBody(_, _)   => s"--data-binary <PLACEHOLDER>"
      case InputStreamBody(_, _)  => s"--data-binary <PLACEHOLDER>"
      case StreamBody(_)          => s"--data-binary <PLACEHOLDER>"
      case MultipartBody(_)       => s"--data-binary <PLACEHOLDER>"
      case FileBody(file, _)      => s"""--data-binary @${file.name}"""
      case NoBody                 => ""
    }
  }

  private def extractOptions(r: R): String = {
    val sb = mutable.ListBuffer[String]()
    if (r.options.followRedirects) {
      sb.append("-L")
    }
    sb.append(s"--max-redirs=${r.options.maxRedirects}")
    sb.mkString(" ")
  }

  private def addSpaceIfNotEmpty(fInput: R => String): R => String =
    t => if (fInput(t).isEmpty) "" else s" ${fInput(t)}"
}

object ToCurlConverter {
  def requestToCurl[R <: Request[_, _]]: ToCurlConverter[R] = new ToCurlConverter[R]
}
