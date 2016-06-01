package com.foofv.crawler.schedule

import redis.ByteStringSerializer
import redis.ByteStringDeserializer
import com.foofv.crawler.redis.cli.RedisOps
import com.foofv.crawler.CrawlerConf

/**
 * @author soledede
 * @email wengbenjue@163.com
 * 
 */
private[crawler]
class RedisSetTaskQueueImpl private (conf: CrawlerConf)  extends ITaskQueue {
    val redis: RedisOps = RedisOps.createRedis("akka", conf)

  override def putToSet[T: ByteStringSerializer](key: String, members: T):Boolean = redis.putToSet[T](key, members)

  override def getAllFromSetByKey[T: ByteStringDeserializer](key: String):Seq[T] = redis.getAllFromSetByKey[T](key)
  
  override def removeElementFromSetByKey[T: ByteStringSerializer](key: String, members: Seq[T]):Boolean = redis.removeElementFromSetByKey(key, members)
  
}

object RedisSetTaskQueueImpl {
  var set: RedisSetTaskQueueImpl = null
  def apply(conf: CrawlerConf): RedisSetTaskQueueImpl = {
    if (set == null) set = new RedisSetTaskQueueImpl(conf)
    set
  }

}

object testRedisSetTaskQueueImpl{
  def main(args: Array[String]): Unit = {
    val redisSet = RedisSetTaskQueueImpl(new CrawlerConf)
    redisSet.putToSet("t", "qq1")
    redisSet.putToSet("t", "qq2")
    redisSet.putToSet("t", "qq3")
    
    redisSet.removeElementFromSetByKey("t", Seq("qq1","qq2"))
    
    println(redisSet.getAllFromSetByKey("t"))
  }
}