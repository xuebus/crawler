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

import scala.reflect.ClassTag
import com.foofv.crawler.entity.CrawlerTaskEntity
import redis.ByteStringSerializer
import redis.ByteStringDeserializer
import com.foofv.crawler.CrawlerConf

/**
 * @author soledede
 * @email wengbenjue@163.com
 * the interface of the uri queue,you can use redis,filesystem,memcahce ...
 */
private[crawler] trait ITaskQueue {

  /**
   * get the first element of the queue
   */
  def getFirstElement[T: ByteStringDeserializer](name: String): T = {null.asInstanceOf[T]}

  /**
   * get the last element of the queue
   */
  def getLastElement[T: ByteStringDeserializer](name: String): T = {null.asInstanceOf[T]}

  /**
   * get the first element of the queue
   */
  def getFirstElementWithScore[T: ByteStringDeserializer](name: String): (T, Double) = null.asInstanceOf[(T, Double)]

  /**
   * get the last element of the queue
   */
  def getLastElementWithScore[T: ByteStringDeserializer](name: String): (T, Double) = null.asInstanceOf[ (T, Double)]

  /**
   * delete the first element of the queue
   */
  def delFirstElement(key: String): Boolean = false

  /**
   * delete the last element of the queue
   */
  def delLastElement(key: String): Boolean = false

  /**
   * add element to the sort set queue
   */
  def putElementToSortedSet[T: ByteStringSerializer](name: String, task: T, score: Double): Boolean = false

  /**
   * add element to the queue
   */
  def putElementToList[T: ByteStringSerializer](name: String, task: T): Boolean = false

  /**
   * get task by iteration
   */
  def next[T: ByteStringDeserializer](): T = null.asInstanceOf[T]

  /**
   * get counter next value
   */
  def getCounterNextVal(key: String): Long = {
    var r = -1L
    r
  }

  /**
   * put key-value HashMap
   */
  def putKvHashMap[T: ByteStringSerializer](key: String, keysValues: Map[String, T]): Boolean = {
    var r = false
    r
  }

  /**
   * get key-value HashMap
   */
  def getKvHashMap(key: String, fields: Seq[String]): Seq[String] = {
    var r = null.asInstanceOf[Seq[String]]
    r
  }

  /**
   * get Seq[T] by key and fields
   */
  def getHashmapByKeyField[T: ByteStringDeserializer](key: String, fields: Seq[String]): Seq[Option[T]] = {
    null.asInstanceOf[Seq[Option[T]]]
  }
  
  def putToSet[T: ByteStringSerializer](key: String, member: T):Boolean = {false}
  
  def removeElementFromSetByKey[T: ByteStringSerializer](key: String, members: Seq[T]):Boolean = {false}

  def getAllFromSetByKey[T: ByteStringDeserializer](key: String):Seq[T] = {null.asInstanceOf[Seq[T]]}

}

object ITaskQueue {

  def apply(shcheduleQueueType: String, conf: CrawlerConf): ITaskQueue = {
    shcheduleQueueType match {
      case "sortSet" => RedisSortedSetTaskQueueImpl(conf)
      case "list"    => RedisListTaskQueueImpl(conf)
      case "map"     => RedisMapTaskQueueImpl(conf)
      case "set"     => RedisSetTaskQueueImpl(conf)
      case _         => null
    }
  }
}

private[crawler] trait ITaskQueueFactory {

  def createTaskQueue(): ITaskQueue
}
