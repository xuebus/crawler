package com.foofv.crawler.agent

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.schedule.{ ITaskQueue, RedisSortedSetTaskQueueImpl, RedisListTaskQueueImpl }
import com.foofv.crawler.enumeration._
import com.foofv.crawler.util.constant.Constant
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.util.concurrent.TimeUnit
import com.foofv.crawler.redis.cli.RedisOps
import redis.ByteStringSerializer
import redis.ByteStringDeserializer

//get CrawlerTaskEntity from Redis.SortedSet
//put CrawlerTaskEntity to  Redis.list
class CrawlerDTGetTaskFromSortedSet(conf: CrawlerConf,
                                    name: String = "GetTaskFromSortedSet")
  extends CrawlerDTWorkerTest(conf, name) {

  // initialize
  private val taskQueue = ITaskQueue("sortSet", conf)
  private val taskList = ITaskQueue("list", conf)
  private val taskSortedSetKey = Constant(conf).CRAWLER_TASK_SORTEDSET_KEY
  private val taskListKey = Constant(conf).CRAWLER_TASK_LIST_KEY
  
  val redis: RedisOps = RedisOps.createRedis("akka", conf)
  private val key = Constant(conf).CRAWLER_GET_NEXT_TASK_INTERVAL_KEY
  
  val cacheLoader: CacheLoader[java.lang.String, java.lang.Long] =
    new CacheLoader[java.lang.String, java.lang.Long]() {
      def load(key: java.lang.String): java.lang.Long = {
        1000
      }
    }
  val getNextTaskIntervalCacheManager = CacheBuilder.newBuilder()
    .expireAfterWrite( 10 * 1000, TimeUnit.MILLISECONDS).build(cacheLoader)
    
  private def getGetNextTaskInterval(key: String): Long = {
    var interval = getNextTaskIntervalCacheManager.getIfPresent(key)
    if (interval == null) {
      interval = redis.getValue[String](key).getOrElse("200").toLong
      getNextTaskIntervalCacheManager.put(key, interval)
    }
    logInfo(s"interval[$interval]")
    interval
  }
  
  override def callback = {
    var newPeriod = this.getDefaultIntervaltime
    logDebug("GetTaskFromSortedSet START")
    val result = Option[(CrawlerTaskEntity, Double)](taskQueue.getFirstElementWithScore[CrawlerTaskEntity](taskSortedSetKey))
    val task = result.get._1
    if (task == null) {
      logDebug("tasks SortedSet is empty!")
    } else {
      val score = result.get._2
      val currentTime = System.currentTimeMillis()
      if (currentTime >= score) {
        //TODO tx
        logDebug("##########\n" + task.toString())
        taskList.putElementToList(taskListKey, task)
        logDebug("add task [" + task.taskId + "] to task List")
        taskQueue.delFirstElement(taskSortedSetKey)
        logDebug("remove task [" + task.taskId + "] from TaskSortedQueue")
        newPeriod = getGetNextTaskInterval(key)
      } else {
    	  logDebug("score " + score.toLong + ",retry it later...")
        //val taskId = task.taskId
        //newPeriod = (score - currentTime).toLong + 200
        //logDebug(s"task[$taskId] should be execute in [$newPeriod]milliseconds, just wait...")
      }
    }
    logDebug("GetTaskFromSortedSet END")
    newPeriod
  }

}

object TestCrawlerDTGetTaskFromSortedSet {
  def main(args: Array[String]): Unit = {
    val t = new CrawlerDTGetTaskFromSortedSet(new CrawlerConf())
    t.startUp()
  }

}