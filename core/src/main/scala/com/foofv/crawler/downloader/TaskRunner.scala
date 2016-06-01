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

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.control.CrawlerControlImpl
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.util.Logging

/**
 * TaskRunner for fetch
 * We need add it to thread pool
 * @author soledede
 */
private[crawler]
class TaskRunner(taskEntity: CrawlerTaskEntity, downlaoder: Downloader) extends Runnable with Logging {

  override def run() {
    try {
      downlaoder.asynfetch(taskEntity)
    } catch {
      case t: Throwable => logError("TaskRunner Error", t)
    }
  }

}

object Main extends App {

  val downloader = Downloader("http", new CrawlerConf())
  val taskEntity = new CrawlerTaskEntity()
//  taskEntity.httpRefer = "http://developer.baidu.com/map/wiki/index.php?title=car/api/geocoding"
//  taskEntity.userAgent="Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0"
  taskEntity.taskURI = "http://api.map.baidu.com/telematics/v3/reverseGeocoding?location=12960622.94,4843926.42&coord_type=bd09mc&ak=AOkR59MT4atImiOo3BGee0lL&output=json"
  //  new Thread(new TaskRunner(taskEntity, downloader))
  downloader.asynfetch(taskEntity)
}