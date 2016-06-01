/**
 *Copyright [2015] [soledede]
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
**/
package com.foofv.crawler

import com.foofv.crawler.util.Logging
import com.foofv.crawler.serializer.Serializer

/**
 * @author soledede
 */
private[crawler]
class CrawlerEnv (val conf: CrawlerConf,val serializer: Serializer) extends Logging {
}


object CrawlerEnv extends Logging {
  @volatile private var env: CrawlerEnv = _


  def set(e: CrawlerEnv) {
    env = e
  }

  def get: CrawlerEnv = {
    env
  }

  def init(conf: CrawlerConf,ser: Serializer ){
    new CrawlerEnv(conf,ser)
  }
  }


