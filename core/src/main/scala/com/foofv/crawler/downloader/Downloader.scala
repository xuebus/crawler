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

import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.CrawlerConf
import org.apache.http.client.protocol.HttpClientContext

/**
 * Downloader are called by TaskRunner that use for Thread Pool,so you need to care about thread safe
 * You can implement HttpDownloader or ApiDownloader
 * When you have finished ,you need persist the data object to the hbase or other storage and send the rowkey to kafka
 * @author soledede
 */
private[crawler]
trait Downloader {
  
  //start fetch 
  def fetch(taskEntity: CrawlerTaskEntity): ResObj
  
  def asynfetch(taskEntity: CrawlerTaskEntity)
  
  //save result object
  def persist(resObj: ResObj): Boolean
  
  //publish rowkey to kafaka
  def product(identity: String)

}

object Downloader{
  def apply(downloaderType: String, conf: CrawlerConf): Downloader= {
    downloaderType match {
      case "http" => HttpDownloader(conf)
      case _ => null
    }
  }
}