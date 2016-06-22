package com.foofv.crawler.downloader

/**
 * @author soledede
 */
private[crawler]
trait RequestFactory {

  def createRequest(): Request
}