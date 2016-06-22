package com.foofv.crawler.downloader

import java.net.URI
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.CrawlerConf
import org.apache.http.client.protocol.HttpClientContext

/**
 * @author soledede
 */
private[crawler]
trait Request {
  
  def download(taskEntity: CrawlerTaskEntity): Response
  
  def download(taskEntity: CrawlerTaskEntity, callback: (HttpClientContext, org.apache.http.HttpResponse) => Unit)
}

object Request{
   def apply(httpRequestType: String, conf: CrawlerConf): Request= {
    httpRequestType match {
      case "httpClient" => HttpClientRequest(conf)
      case _ => null
    }
  }
  
}