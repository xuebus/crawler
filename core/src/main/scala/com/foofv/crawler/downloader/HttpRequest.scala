package com.foofv.crawler.downloader

import java.net.URI
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.enumeration.HttpRequestMethodType
import com.foofv.crawler.util.Logging
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpHost
import com.foofv.crawler.schedule.ITaskQueueFactory
import com.foofv.crawler.util.LogOper
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.antispamming.IAntiSpammingFactory

private[crawler] abstract class HttpRequest extends Request with Logging {

  def download(taskEntity: CrawlerTaskEntity): Response

  protected def getHttpProxy(proxyHost: String, proxyPort: Int): HttpHost = {
    //TODO 
    //null
    //"202.43.147.226" 8080

    new HttpHost(proxyHost, proxyPort, null)
  }

}

