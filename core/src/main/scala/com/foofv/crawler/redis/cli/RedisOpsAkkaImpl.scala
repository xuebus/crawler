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

import com.foofv.crawler.util.constant.Constant

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.foofv.crawler.CrawlerConf
import redis.ByteStringDeserializer
import redis.ByteStringSerializer
import redis.RedisClient
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.util.MyHashMap
import com.foofv.crawler.util.Logging

private[crawler] class RedisOpsAkkaImpl private(conf: CrawlerConf) extends RedisOps with Logging {

  var redis: RedisClient = _

  if (!conf.getBoolean("local", false)) {
    implicit val akkaSystem = akka.actor.ActorSystem()
    //val redis = RedisClient(conf.get("crawler.redis.ip", "192.168.1.203"))
    redis = RedisClient(port = conf.getInt("crawler.redis.port", 6379), host = conf.get("crawler.redis.ip", Constant(conf).REDIS_SERVER_IP))
  }


  override def putToSortedSet[T: ByteStringSerializer](name: String, value: T, score: Double): Boolean = {
    this.synchronized {
      var r = false
      try {
        val result = redis.zadd(name, (score, value))
        Await.result(result, 10 seconds)
        if (result.value.get.get == 1) r = true
      } catch {
        case e: Exception => logError("put data to redis sortedset failed！" + e.getMessage())
      }
      r
    }
  }

  override def getFirstFromSortedSet[T: ByteStringDeserializer](name: String): (T, Double) = {
    //   implicit val akkaSystem = akka.actor.ActorSystem()
    // val redis = RedisClient(conf.get("crawler.redis.ip", "192.168.1.203"))
    // this.synchronized {
    var rsObj = null.asInstanceOf[T]
    var scores: Double = 0.0
    try {
      val r = for {
        getOpt <- redis.zrangeWithscores[T](name, 0, 0)
      } yield {
        getOpt.map(obj => {
          rsObj = obj._1
          scores = obj._2
        })
      }
      Await.result(r, 10 seconds)
    } catch {
      case e: Exception => logError("get first data from redis sortedset failed！", e)
    }
    (rsObj, scores)
    // }
  }

  def getLastFromSortedSet[T: ByteStringDeserializer](name: String): (T, Double) = {
    //this.synchronized {
    var rsObj = null.asInstanceOf[T]
    var scores: Double = 0.0
    try {
      val r = for {
        getOpt <- redis.zrangeWithscores[T](name, -1, -1)
      } yield {
        getOpt.map(obj => {
          rsObj = obj._1
          scores = obj._2
        })
      }
      Await.result(r, 10 seconds)
    } catch {
      case e: Exception => logError("get last data from redis sortedset failed！" + e.getMessage())
    }
    (rsObj, scores)
    //}
  }

  override def rmLastFromSortedSet(key: String): Boolean = {
    this.synchronized {
      var r = false
      try {
        val result = redis.zremrangebyrank(key, -1, -1)
        Await.result(result, 10 seconds)
        if (result.value.get.get == 1) r = true
      } catch {
        case e: Exception => logError("del last data from redis sortedset failed！" + e.getMessage())
      }
      r
    }
  }

  override def rmFirstFromSortedSet(key: String): Boolean = {
    this.synchronized {
      var r = false
      try {
        val result = redis.zremrangebyrank(key, 0, 0)
        Await.result(result, 10 seconds)
        if (result.value.get.get == 1) r = true
      } catch {
        case e: Exception => logError("del first data from redis sortedset failed！" + e.getMessage())
      }
      r
    }
  }

  override def putToList[T: ByteStringSerializer](key: String, values: T): Boolean = {
    this.synchronized {
      var r = false
      try {
        val result = redis.lpush[T](key, values)
        if (result.value.get.get == 1) r = true
        Await.result(result, 10 seconds)
        println(result)
      } catch {
        case e: Exception => logError("put data to redis list failed！ " + e.getMessage)
      }
      r
    }
  }

  override def popFirstFromList[T: ByteStringDeserializer](key: String): T = {
    // this.synchronized {
    var rsObj = null.asInstanceOf[T]
    try {
      val r = for {
        getOpt <- redis.lpop[T](key)
      } yield {
        getOpt.map(getObj => {
          rsObj = getObj
        })
      }
      Await.result(r, 10 seconds)
    } catch {
      case e: Exception => logError("get first data from redis list failed！" + e.getMessage())
    }
    rsObj
    // }
  }

  def popLastFromList[T: ByteStringDeserializer](key: String): T = {
    // this.synchronized {
    var rsObj = null.asInstanceOf[T]
    try {
      val r = for {
        getOpt <- redis.rpop[T](key)
      } yield {
        getOpt.map(getObj => {
          rsObj = getObj
        })
      }
      Await.result(r, 10 seconds)
    } catch {
      case e: Exception => logError("get last data from redis list failed！" + e.getMessage())
    }
    rsObj
    //}
  }

  def getCounterNextVal(key: String): Long = {
    this.synchronized {
      var r = -1L
      try {
        val result = redis.incr(key)
        Await.result(result, 10 seconds)
        r = result.value.get.get
      } catch {
        case e: Exception => logError("get counter next value failed！" + e.getMessage())
      }
      r
    }
  }

  def putKvHashMap[T: ByteStringSerializer](key: String, keysValues: Map[String, T]): Boolean = {
    this.synchronized {
      var r = false
      try {
        val result = redis.hmset(key, keysValues)
        Await.result(result, 10 seconds)
        r = result.value.get.get
      } catch {
        case e: Exception => logError("put key-value HashMap to Redis failed！" + e.getMessage())
      }
      r
    }
  }

  def getKvHashMap(key: String, fields: Seq[String]): Seq[String] = {
    //this.synchronized {
    var r = null.asInstanceOf[Seq[String]]
    try {
      val result = redis.hmget(key, fields: _*)
      Await.result(result, 10 seconds)
      r = result.value.get.get.map { ele => ele.get.utf8String }
    } catch {
      case e: Exception => logError("get key-value HashMap from Redis failed！" + e.getMessage())
    }
    r
    //}
  }

  def getHashmapByKeyField[T: ByteStringDeserializer](key: String, fields: Seq[String]): Seq[Option[T]] = {
    //this.synchronized {
    var r = null.asInstanceOf[Seq[Option[T]]]
    try {
      val result = redis.hmget(key, fields: _*)
      Await.result(result, 10 seconds)
      r = result.value.get.get
    } catch {
      case e: Exception => logError("get key-value HashMap from Redis failed！" + e.getMessage())
    }
    r
    //}
  }

  def getHashmapByKeyField2[T: ByteStringDeserializer](key: String, fields: Seq[String]): Seq[Option[T]] = {
    //this.synchronized {
    var r = null.asInstanceOf[Seq[Option[T]]]
    try {
      val result = redis.hmget(key, fields: _*)
      Await.result(result, 10 seconds)
      r = result.value.get.get
    } catch {
      case e: Exception => logError("get key-value HashMap from Redis failed！" + e.getMessage())
    }
    r
    //}
  }

  def setValue[T: ByteStringSerializer](key: String, value: T): Boolean = {
    this.synchronized {
      var r = null.asInstanceOf[Boolean]
      try {
        val result = redis.set(key, value)
        Await.result(result, 10 seconds)
        r = result.value.get.get
        logInfo("set (" + key + " -> " + value + ")")
      } catch {
        case e: Exception => logError("set key -> value failed！", e)
      }
      r
    }
  }

  // redis command: SETEX key seconds value
  def setValue[T: ByteStringSerializer](key: String, value: T, exSeconds: Long): Boolean = {
    this.synchronized {
      var r = null.asInstanceOf[Boolean]
      try {
        val result = redis.set(key, value, Option(exSeconds))
        Await.result(result, 10 seconds)
        r = result.value.get.get
        logInfo("set (" + key + " -> " + value + ")")
      } catch {
        case e: Exception => logError("set key -> value failed！", e)
      }
      r
    }
  }

  def getValue[T: ByteStringDeserializer](key: String): Option[T] = {
    //this.synchronized {
    var r = null.asInstanceOf[Option[T]]
    try {
      val result = redis.get(key)
      Await.result(result, 10 seconds)
      r = result.value.get.get
      logInfo("get (" + key + " -> " + r + ")")
    } catch {
      case e: Exception => logError("get key -> value failed！", e)
    }
    r
    //}
  }

  def delKey(keys: Seq[String]): Long = {
    this.synchronized {
      var r = null.asInstanceOf[Long]
      try {
        val result = redis.del(keys: _*)
        Await.result(result, 10 seconds)
        r = result.value.get.get
        logInfo("del " + keys)
      } catch {
        case e: Exception => logError("del keys failed！", e)
      }
      r
    }
  }


  def putToSet[T: ByteStringSerializer](key: String, member: T): Boolean = {
    this.synchronized {
      var r = null.asInstanceOf[Boolean]
      try {
        val result = redis.sadd(key, member)
        Await.result(result, 10 seconds)
        if (result.value.get.get == 1) r = true
        logInfo("sadd (" + key + " -> " + member + ")")
      } catch {
        case e: Exception => logError("set key -> value failed！", e)
      }
      r
    }
  }

  def removeElementFromSetByKey[T: ByteStringSerializer](key: String, members: Seq[T]): Boolean = {
    this.synchronized {
      var r = null.asInstanceOf[Boolean]
      try {
        val result = redis.srem(key, members: _*)
        Await.result(result, 10 seconds)
        if (result.value.get.get == 1) r = true
        logInfo("srem (" + key + " -> " + members + ")")
      } catch {
        case e: Exception => logError("srem key -> value failed！", e)
      }
      r
    }
  }

  def getAllFromSetByKey[T: ByteStringDeserializer](key: String): Seq[T] = {
    // this.synchronized {
    var r: Seq[T] = null
    try {
      val result = redis.smembers(key)
      Await.result(result, 10 seconds)
      r = result.value.get.get
      logInfo("get (" + key + " -> " + r + ")")
    } catch {
      case e: Exception => logError("get key -> value failed！", e)
        null
    }
    r
    //}
  }


  override def incrBy(key: String, step: Int): Int = {
    this.synchronized {
      var r = -1
      try {
        val result = redis.incrby(key, step)
        Await.result(result, 20 seconds)
        r = result.value.get.get.toInt
      } catch {
        case e: Exception => logError("get counter by step value failed！" + e.getMessage())
      }
      r
    }
  }

  def keys(parttern: String): Option[Seq[String]] = {
    var r: Option[Seq[String]] = None
    try {
      val result = redis.keys(parttern)
      Await.result(result, 20 seconds)
      r = result.value.get.toOption
    } catch {
      case e: Exception => logError("get counter by step value failed！" + e.getMessage())
    }
    r
  }


}

object RedisOpsAkkaImpl {
  var redisAkka: RedisOpsAkkaImpl = null

  def apply(conf: CrawlerConf): RedisOpsAkkaImpl = {
    if (redisAkka == null)
      redisAkka = new RedisOpsAkkaImpl(conf)
    redisAkka
  }
}

object Test {
  def main(args: Array[String]): Unit = {
    testsetValueWithExpiredtime
  }

  def testPutToHashMap() {
    val redis: RedisOpsAkkaImpl = RedisOpsAkkaImpl(new CrawlerConf)
    val key = "test-hmset"
    val entity = new com.foofv.crawler.entity.CrawlerTaskEntity
    val keysValues: Map[String, AnyRef] = Map("name" -> "test-name", "age" -> "50", "entity" -> entity)
    var r1 = redis.putKvHashMap[CrawlerTaskEntity]("sdfs", Map("a" -> entity))
    println(r1)
    val r2 = redis.getKvHashMap("sdfs", Seq("a"))
    println(r2)
  }

  def testPutToHashMap2() {
    val redis: RedisOpsAkkaImpl = RedisOpsAkkaImpl(new CrawlerConf)
    val key = "test-hmset"
    val hashMap = new MyHashMap[String, String]
    hashMap("a") = "AAA"
    hashMap("a2") = "AAA2"
    var r1 = redis.putKvHashMap[MyHashMap[String, String]]("sdfs", Map("map" -> hashMap))
    println(r1)
    val r2 = redis.getHashmapByKeyField[MyHashMap[String, String]]("sdfs", Seq("map"))
    println(r2)
  }

  def testSetValue() {
    val redis: RedisOpsAkkaImpl = RedisOpsAkkaImpl(new CrawlerConf)
    val key = "test-set"
    val r1 = redis.setValue(key, "test-value222")
    println(r1 + "---r1")
    val r2 = redis.getValue[String](key)
    println(r2 + "---r2")
  }

  def testsetValueWithExpiredtime() {
    val redis: RedisOpsAkkaImpl = RedisOpsAkkaImpl(new CrawlerConf)
    val key = "test-setex"
    val retValue = redis.setValue(key, "test-value222", 3)
    println(retValue + "---retValue")
    val retValue1 = redis.getValue[String](key)
    println(retValue1 + "---retValue1")
    println("sleep 2 seconds")
    Thread.sleep(2000)
    val retValue2 = redis.getValue[String](key)
    println(retValue2 + "---retValue2")
    Thread.sleep(2000)
    val retValue3 = redis.getValue[String](key)
    println(retValue3 + "---retValue3")
  }
}
