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