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
package com.foofv.crawler.redis.cli

import com.foofv.crawler.util.Logging
import redis.ByteStringSerializer
import redis.ByteStringDeserializer
import com.foofv.crawler.CrawlerConf

/**
 * Communicate with redis
 * @author soledede
 */
private[crawler] trait RedisOps extends Logging {

  def putToSortedSet[T: ByteStringSerializer](name: String, value: T, score: Double): Boolean

  def getFirstFromSortedSet[T: ByteStringDeserializer](name: String): (T, Double)

  def getLastFromSortedSet[T: ByteStringDeserializer](name: String): (T, Double)

  def putToList[T: ByteStringSerializer](key: String, values: T): Boolean

  def popFirstFromList[T: ByteStringDeserializer](key: String): T

  def popLastFromList[T: ByteStringDeserializer](key: String): T

  def rmFirstFromSortedSet(key: String): Boolean

  def rmLastFromSortedSet(key: String): Boolean

  def getCounterNextVal(key: String): Long

  def putKvHashMap[T: ByteStringSerializer](key: String, keysValues: Map[String, T]): Boolean

  def getKvHashMap(key: String, fields: Seq[String]): Seq[String]

  def getHashmapByKeyField[T: ByteStringDeserializer](key: String, fields: Seq[String]): Seq[Option[T]]

  def setValue[T: ByteStringSerializer](key: String, value: T): Boolean

  def setValue[T: ByteStringSerializer](key: String, value: T, exSeconds: Long): Boolean

  def getValue[T: ByteStringDeserializer](key: String): Option[T]

  def delKey(keys: Seq[String]): Long

  def putToSet[T: ByteStringSerializer](key: String, members: T): Boolean

  def removeElementFromSetByKey[T: ByteStringSerializer](key: String, members: Seq[T]): Boolean

  def getAllFromSetByKey[T: ByteStringDeserializer](key: String): Seq[T]

  def incrBy(key: String, step: Int): Int

  def keys(parttern: String): Option[Seq[String]]

}

object RedisOps {

  def apply(redisOpsType: String, conf: CrawlerConf): RedisOps = {
    createRedis(redisOpsType, conf)
  }

  def createRedis(redisOpsType: String, conf: CrawlerConf): RedisOps = {
    redisOpsType match {
      case "akka" => RedisOpsAkkaImpl(conf)
      case _ => null
    }
  }
}