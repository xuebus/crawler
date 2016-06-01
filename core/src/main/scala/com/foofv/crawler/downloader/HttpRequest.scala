/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

