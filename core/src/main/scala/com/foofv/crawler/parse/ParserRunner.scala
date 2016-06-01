/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foofv.crawler.parse

import com.foofv.crawler.storage.StorageManagerFactory
import com.foofv.crawler.storage.StorageManager
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Logging
import com.foofv.crawler.entity.ResObj

/**
 * the thread of parser
 * @author soledede
 */
private[crawler] class ParserRunner(resObj: ResObj, conf: CrawlerConf) extends Runnable with ParserManagerFactory with Logging {

  override def createParserManager(): ParserManager = {
    ParserManager(conf)
  }

  override def run = {
    createParserManager.start(resObj)
  }
}