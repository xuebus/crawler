
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
package com.foofv.crawler.queue

import com.foofv.crawler.CrawlerConf

/**
 * You can implements by kafka,metaq...
 * @author soledede
 */
private[crawler] trait MessageQueue {

  def sendMsg(msg: String): Boolean

  def consumeMsg(): String
  
  def sendMsgLocal(message: String): Boolean

  def size(): Int
  
  def start()
}

object MessageQueue {
  def apply(msgQueueType: String, conf: CrawlerConf): MessageQueue = {
    msgQueueType match {
      case "kafka" => KafkaMessageQueue(conf)
      case _       => null
    }
  }
}

private[crawler] trait IMessageQueueFactory {
  def createMessageQueue: MessageQueue
}