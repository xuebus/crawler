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
package com.foofv.crawler.schedule

import scala.concurrent.ExecutionContext.Implicits.global

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity._
import com.foofv.crawler.redis.cli.RedisOps
import com.foofv.crawler.redis.cli.RedisOpsAkkaImpl
import com.foofv.crawler.util.Logging

import redis.ByteStringDeserializer
import redis.ByteStringFormatter
import redis.ByteStringSerializer

/**
 * Schedule task according by Redis
 * @author soledede
 */
private[crawler] class RedisSortedSetTaskQueueImpl private (conf: CrawlerConf) extends ITaskQueue with Logging {
  val redis: RedisOps = RedisOps.createRedis("akka", conf)

  override def getFirstElementWithScore[T: ByteStringDeserializer](name: String): (T, Double) = {
    redis.getFirstFromSortedSet[T](name)
  }

  override def getLastElementWithScore[T: ByteStringDeserializer](name: String): (T, Double) = {
    redis.getLastFromSortedSet[T](name)
  }

  override def getFirstElement[T: ByteStringDeserializer](name: String): T = {
    null.asInstanceOf[T]
  }

  override def getLastElement[T: ByteStringDeserializer](name: String): T = {
    null.asInstanceOf[T]
  }

  override def delFirstElement(key: String): Boolean = {
    redis.rmFirstFromSortedSet(key)
  }

  override def delLastElement(key: String): Boolean = {
    redis.rmLastFromSortedSet(key)
  }

  override def putElementToSortedSet[T: ByteStringSerializer](name: String, task: T, score: Double): Boolean = {
    redis.putToSortedSet[T](name, task, score)
  }

  override def putElementToList[T: ByteStringSerializer](name: String, task: T): Boolean = {
    false
  }

  override def next[T: ByteStringDeserializer](): T = {
    new CrawlerTaskEntity().asInstanceOf[T]
  }

  override def getCounterNextVal(key: String): Long = {
    redis.getCounterNextVal(key)
  }

  override def putKvHashMap[T: ByteStringSerializer](key: String, keysValues: Map[String, T]): Boolean = {
    redis.putKvHashMap(key, keysValues)
  }

  override def getKvHashMap(key: String, fields: Seq[String]): Seq[String] = {
    redis.getKvHashMap(key, fields)
  }

}

object RedisSortedSetTaskQueueImpl {
  var redisSortSet: RedisSortedSetTaskQueueImpl = null
  def apply(conf: CrawlerConf): RedisSortedSetTaskQueueImpl = {
    if (redisSortSet == null) redisSortSet = new RedisSortedSetTaskQueueImpl(conf)
    redisSortSet
  }

}

object test {
  def main(args: Array[String]): Unit = {
    /*val conf = new CrawlerConf()
    val red =  new RedisSortedSetTaskQueueImpl(conf)
    red.putElementToSortedSet("", task, score)
    println(red.getFirstElement[DumbClass]("zdum"))
    val t = new RedisOpsAkkaImpl(conf)
    println(t.getFirstFromSortedSet[DumbClass]("zdum"))
    println("==================================")
    val task =  new CrawlerTaskEntity()
    red.putElementToSortedSet("crawler", task, 21.0)*/

    RedisSortedSetTaskQueueImpl(new CrawlerConf()).putElementToSortedSet[CrawlerTaskEntity]("", new CrawlerTaskEntity, 42)
    //testGetCounterNextVal()
    // testPutKvHashMap()
  }

  def testPutKvHashMap() {
    val conf = new CrawlerConf()
    val red = RedisSortedSetTaskQueueImpl(conf)
    val key = "test-hmset"
    val entity = new com.foofv.crawler.entity.CrawlerTaskEntity
    val r1 = red.putKvHashMap(key, Map("entity" -> entity))
    println(r1)
    val r2 = red.getKvHashMap(key, Seq("entity"))
    println(r2.head)
  }

  def testGetCounterNextVal() {
    val threadPool = com.foofv.crawler.util.Util.newDaemonFixedThreadPool(20, "test");
    val conf = new CrawlerConf()
    val red = RedisSortedSetTaskQueueImpl(conf)
    for (i <- 1 to 50) {
      threadPool.execute(new Test(i, red))
    }
    threadPool.shutdown();
  }
  private class Test(index: Int, taskQueue: ITaskQueue) extends Runnable {
    def run() {
      val result = taskQueue.getCounterNextVal("test")
      printf("%d -- %d\n", index, result)
    }
  }
}