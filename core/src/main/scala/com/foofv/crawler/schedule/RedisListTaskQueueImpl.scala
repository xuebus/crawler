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

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.redis.cli.RedisOps
import com.foofv.crawler.redis.cli.RedisOpsAkkaImpl
import com.foofv.crawler.util.Logging
import redis.ByteStringSerializer
import redis.ByteStringDeserializer

/**
 * @author soledede
 */
private[crawler] class RedisListTaskQueueImpl private (conf: CrawlerConf) extends ITaskQueue with Logging {

  val redis: RedisOps = RedisOps.createRedis("akka", conf)

  override def getFirstElementWithScore[T: ByteStringDeserializer](name: String): (T, Double) = {
    null.asInstanceOf[(T, Double)]
  }

  override def getLastElementWithScore[T: ByteStringDeserializer](name: String): (T, Double) = {
    null.asInstanceOf[(T, Double)]
  }

  override def getFirstElement[T: ByteStringDeserializer](name: String): T = {
    redis.popFirstFromList[T](name)
  }

  override def getLastElement[T: ByteStringDeserializer](name: String): T = {
    redis.popLastFromList(name)
  }
  override def delFirstElement(key: String): Boolean = {
    false
  }

  override def delLastElement(key: String): Boolean = {
    false
  }

  override def putElementToSortedSet[T: ByteStringSerializer](name: String, task: T, score: Double): Boolean = {
    false
  }

  override def putElementToList[T: ByteStringSerializer](name: String, tast: T): Boolean = {
    redis.putToList[T](name, tast)
  }

  override def next[T: ByteStringDeserializer](): T = {
    new CrawlerTaskEntity().asInstanceOf[T]
  }
}

object RedisListTaskQueueImpl {
  var redisList: RedisListTaskQueueImpl = null
  def apply(conf: CrawlerConf): RedisListTaskQueueImpl = {
    if (redisList == null) redisList = new RedisListTaskQueueImpl(conf)
    redisList
  }

  def main(args: Array[String]): Unit = {
    val conf = new CrawlerConf()
    val red = RedisListTaskQueueImpl(conf)
    //red.putElementToSortedSet("", task, score)
    // println(red.getFirstElement[DumbClass]("zdum"))
    // val t = new RedisOpsAkkaImpl(conf)
    //println(t.getFirstFromSortedSet[DumbClass]("zdum"))
    //println("==================================")
    //val task =  new CrawlerTaskEntity()
    // red.putElementToList("crlist", task)

  }
}
