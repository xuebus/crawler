package com.foofv.crawler.control

import com.foofv.crawler.entity.Job
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.enumeration._
import com.foofv.crawler.rule.ITaskFilter
import com.foofv.crawler.util.Logging
import com.foofv.crawler.redis.cli.RedisOpsAkkaImpl
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.listener.TraceListenerWaiter
import org.apache.commons.lang3.StringUtils
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.JsonNode
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorRef

/**
 * the entrance of crawler
 * @author soledede
 */
private[crawler] trait CrawlerControl extends Logging {

  //initialise 
  def init()
  
  def addSeedTaskByJson(seeds: String): String

  //add the seedTask and distribute the jobId              
  def addSeedTask(
    seed: String, taskType: CrawlerTaskType.Type = CrawlerTaskType.GENERAL_CRAWLER,
    taskDomain: String = "", cookies: String = "", jobName: String = "",
    userAgent: String = CrawlerTaskEntity.DefalutUserAgent,
    charset: String = CrawlerTaskEntity.DefaultCharset,
    retryTime: Int = 3, isUseProxy: Short = 0, intervalTime: Int = 30,
    httpmethod: HttpRequestMethodType.Type = HttpRequestMethodType.GET,
    topciCrawlerParserClassName: String = "null",
    isNeedSaveParserYslf: NeedSaveParser.Door = NeedSaveParser.NO_NEED,
    totalBatch: Int = 1, httpRefer: String = "null", totalDepth: Int = 1): Boolean

  //submit the task to queue for schedule
  def submitTask(task: CrawlerTaskEntity): Boolean

  def addUserAgent(userAgent: String): Unit = {}
  
  def addTaskMeituanTakeoutMerchant(actor: ActorRef, size: Int, startIndex: Int, batchSize: Int, jobId: Long, jobName: String): String
}

private[crawler] object CrawlerControl extends Logging {

  def apply(conf: CrawlerConf): CrawlerControl = {
    CrawlerControlImpl(conf)
  }

}

