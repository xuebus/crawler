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
package com.foofv.crawler.blockqueue

import com.foofv.crawler.util.Logging
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.CrawlerConf
import scala.collection.mutable.HashMap

/**
 * The Blocking Queue of Array,it need a array size
 * @author soledede
 */

private[crawler] class CrawlerLinkedBlockQueueImpl[T] private (conf: CrawlerConf, blockLength: Int) extends CrawlerBlockQueue[T] with Logging {

  def this(conf: CrawlerConf) = this(conf, -1)

  private var blockSize = conf.getInt("crawler.linked.block.queue.block.size", 100000)

  if (blockLength != -1) blockSize = blockLength

  private val taskBlockingQueue = new LinkedBlockingQueue[T](blockSize)

  override def put(task: T): Unit = {
    this.taskBlockingQueue.put(task)
  }

  override def offer(obj: T): Boolean = {
    this.taskBlockingQueue.offer(obj)
  }

  override def take(): T = {
    this.taskBlockingQueue.take()
  }

  override def size(): Int = {
    this.taskBlockingQueue.size()
  }

  override def remove(obj: T): Unit = {
    this.taskBlockingQueue.remove(obj)
  }

  override def toArray(): Array[AnyRef] = {
    taskBlockingQueue.toArray
  }
}

private[crawler] object CrawlerLinkedBlockQueueImpl {

  private var instanceMap = new HashMap[String, AnyRef]()

  def apply[T](bizCode: String, conf: CrawlerConf): CrawlerLinkedBlockQueueImpl[T] = {
    if (!instanceMap.contains(bizCode))
      instanceMap(bizCode) = new CrawlerLinkedBlockQueueImpl[T](conf)
    instanceMap(bizCode).asInstanceOf[CrawlerLinkedBlockQueueImpl[T]]
  }

  def apply[T](bizCode: String, blockLength: Int, conf: CrawlerConf): CrawlerLinkedBlockQueueImpl[T] = {
    if (!instanceMap.contains(bizCode))
      instanceMap(bizCode) = new CrawlerLinkedBlockQueueImpl[T](conf, blockLength)
    instanceMap(bizCode).asInstanceOf[CrawlerLinkedBlockQueueImpl[T]]
  }

}

object testCrawlerLinkedBlockQueue {
  def main(args: Array[String]): Unit = {
    var taskBlockingQueue = new LinkedBlockingQueue[(String, Object)](10000)
    taskBlockingQueue.offer(("hello", new CrawlerConf))
    test
  }

  def test = {
    val conf = new CrawlerConf
    val queue = CrawlerLinkedBlockQueueImpl[CrawlerTaskEntity]("test", conf)
    val queue1 = CrawlerLinkedBlockQueueImpl[String]("test2", conf)
    for (i <- 1 to 10) {
      queue.put(new CrawlerTaskEntity)
      queue1.put("dsd")
      System.err.println("put--" + i)
    }
    while (true) {
      val task = queue.take()
      val task1 = queue1.take()
      System.err.println("take--" + task)
      System.out.println("take1--" + task1)
    }
  }
}
  
