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
package com.foofv.crawler.blockqueue

import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.CrawlerConf

/**
 * Inteface to put and get the task repeatedly ,the queue is blocking queue
 * @author soledede
 */
private[crawler] trait CrawlerBlockQueue[T] {

  def put(task: T): Unit = {}

  def take(): T = { null.asInstanceOf[T] }

  def size(): Int = { -1 }

  def offer(obj: T): Boolean = { false }

  def toArray(): Array[AnyRef]
  
  def remove(obj:T): Unit

}

private[crawler] trait ICrawlerBlockQueueFactory {
  def getCrawlerBlockQueue[T]: CrawlerBlockQueue[T]
}

object CrawlerBlockQueue {
  def apply[T](insType: String, bizCode: String, conf: CrawlerConf): CrawlerBlockQueue[T] = {
    insType match {
      case "linkedBlock" => CrawlerLinkedBlockQueueImpl[T](bizCode, conf)
      case _             => null
    }
  }

  def apply[T](insType: String, bizCode: String, blockLength: Int, conf: CrawlerConf): CrawlerBlockQueue[T] = {
    insType match {
      case "linkedBlock" => CrawlerLinkedBlockQueueImpl[T](bizCode, blockLength, conf)
      case _             => null
    }
  }
}