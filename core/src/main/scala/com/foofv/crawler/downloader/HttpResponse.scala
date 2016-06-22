package com.foofv.crawler.downloader

import java.net.URI

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

private[crawler] class HttpResponse extends Response with Serializable{

  var uri: String = _
  var code: Int = _
  var headers: Map[String, Seq[String]] = _
  var content: Array[Byte] = _
  var contentType: String = _

  override def getURI: String = { uri }

  override def getCode: Int = { code }

  override def getHeader(key: String): Seq[String] = { headers.get(key).get }

  override def getHeaders: Map[String, Seq[String]] = { headers }

  override def setHeaders(headers: Map[String, Seq[String]]) = { this.headers = headers }

  override def getContentType: String = { contentType }

  override def getContent: Array[Byte] = { content }

  override def toString(): String = {
    ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE)
  }

}