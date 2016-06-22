package com.foofv.crawler.downloader

import java.net.URI


/**
 * @author soledede
 */
private[crawler]
trait Response {
  
  def getURI: String
  
  def getCode: Int
  
  def getHeader(key: String): Seq[String]
  
  def getHeaders: Map[String,Seq[String]]
  
  def setHeaders(headers: Map[String,Seq[String]])
  
  def getContentType: String
  
  def getContent: Array[Byte]

}