package sttp.client3

import sttp.model._
import sttp.model.{Header, StatusCode}

import scala.collection.immutable.Seq

trait ResponseMetadata extends HasHeaders {
  def code: StatusCode
  def statusText: String
  def is200: Boolean = code == StatusCode.Ok
  def isSuccess: Boolean = code.isSuccess
  def isRedirect: Boolean = code.isRedirect
  def isClientError: Boolean = code.isClientError
  def isServerError: Boolean = code.isServerError

  override def toString: String = s"ResponseMetadata($code,$statusText,${headersToStringSafe()})"

  protected def headersToStringSafe(sensitiveHeaders: Set[String] = HeaderNames.SensitiveHeaders): Seq[String] =
    headers.map(_.toStringSafe(sensitiveHeaders))
}

object ResponseMetadata {
  def apply(h: Seq[Header], c: StatusCode, st: String): ResponseMetadata =
    new ResponseMetadata {
      override def headers: Seq[Header] = h
      override def code: StatusCode = c
      override def statusText: String = st
    }
}
