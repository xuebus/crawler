package com.foofv.crawler.util.listener

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.redis.cli.RedisOps
import com.foofv.crawler.util.constant.Constant
import com.foofv.crawler.view.control.ControlWebPage

import scala.collection.immutable.HashSet
import scala.collection.mutable

/**
  * Created by soledede on 2015/9/17.
  */
class CrawlerTaskTraceListener(conf: CrawlerConf) extends TraceListener {
  var redisOps: RedisOps = _

  val isLocal = conf.getBoolean("local", false)
  if (!isLocal)
    redisOps = RedisOps("akka", conf)


  override def onJobStart(jobstart: JobStarted): Unit = {
    if (!isLocal)
      redisOps.setValue(CrawlerTaskTraceListener.task_all_task_preffix + jobstart.jobId + CrawlerTaskTraceListener.separator + jobstart.jobName + CrawlerTaskTraceListener.task_suffix, jobstart.seedNum)
  }

  override def onJobTaskFailed(jobTaskFailed: JobTaskFailed): Int = {
    if (!isLocal)
      redisOps.incrBy(CrawlerTaskTraceListener.task_failed_preffix + jobTaskFailed.jobId + CrawlerTaskTraceListener.separator + jobTaskFailed.jobName + CrawlerTaskTraceListener.task_suffix, jobTaskFailed.num)
    else -1
  }

  override def onJobTaskCompleted(jobTaskCompleted: JobTaskCompleted): Int = {
    if (!isLocal)
      redisOps.incrBy(CrawlerTaskTraceListener.task_completed_preffix + jobTaskCompleted.jobId + CrawlerTaskTraceListener.separator + jobTaskCompleted.jobName + CrawlerTaskTraceListener.task_suffix, jobTaskCompleted.num)
    else -1
  }

  override def onJobTaskAdded(jobTaskAdded: JobTaskAdded): Int = {
    if (!isLocal)
      redisOps.incrBy(CrawlerTaskTraceListener.task_all_task_preffix + jobTaskAdded.jobId + CrawlerTaskTraceListener.separator + jobTaskAdded.jobName + CrawlerTaskTraceListener.task_suffix, jobTaskAdded.num)
    else -1
  }

  override def onSearch(keys: Keys): Option[Seq[String]] = {

    if (!isLocal) {
      val allKey = redisOps.keys(keys.parttern)
      allKey match {
        case Some(k) =>
          k.foreach(cacheControlWebPage(_))
        case None => logInfo("no value can get,connect redis maybe timeout!")
      }
      allKey
    } else null

  }

  def cacheControlWebPage(k: String) = {

    val f = k.split(CrawlerTaskTraceListener.separator)
    val c = ControlWebPage.jobInfoCache
    if (k.startsWith(CrawlerTaskTraceListener.task_failed_preffix)) {
      cache(c, k, CrawlerTaskTraceListener.TASK_FAIELD_NUM, f)
    } else if (k.startsWith(CrawlerTaskTraceListener.task_completed_preffix)) {
      cache(c, k, CrawlerTaskTraceListener.TASK_COMPLETED_NUM, f)
    } else if (k.startsWith(CrawlerTaskTraceListener.task_all_task_preffix)) {
      cache(c, k, CrawlerTaskTraceListener.TASK_COMPLETED_NUM, f)
    }
    //ControlWebPage.jobInfoCache(k) = redisOps.incrBy(k, 0).toInt
  }

  def cache(c: scala.collection.mutable.Map[String, mutable.HashMap[(String, String), Int]], k: String, taskType: String, f: Array[String]): Unit = {
    var s: mutable.HashMap[(String, String), Int] = null
    if (c.contains(taskType)) s = c(taskType)
    else {
      s = new mutable.HashMap[(String, String), Int]()
    }
    s((f(2), f(3))) = redisOps.incrBy(k, 0)
    c(taskType) = s
  }

}

object CrawlerTaskTraceListener {
  val CRAWLER_REDIS_KEY_VALUE_TASK_TRACE_PROGRESS = "task_trace__"
  val preffix = CRAWLER_REDIS_KEY_VALUE_TASK_TRACE_PROGRESS
  val task_failed_preffix = preffix + "failed__"
  val task_completed_preffix = preffix + "completed__"
  val task_all_task_preffix = preffix + "total__"
  val task_suffix = "__num"
  val separator = "__"

  val TASK_COMPLETED_NUM = "completed_task"
  val TASK_FAIELD_NUM = "failed_task"
  val TASK_TOTAL_NUM = "total_task"


  def main(args: Array[String]) {
    //testIncyRedis
    testKeys

    //testJobProgress

    //testWeb
  }


  def testJobProgress() = {
    val l = new CrawlerTaskTraceListener(new CrawlerConf())
    println(l.onJobTaskCompleted(JobTaskCompleted("job", "test", 2)))

  }

  def testKeys() = {
    val l = new CrawlerTaskTraceListener(new CrawlerConf())
    println(l.onSearch(Keys("task_trace__*")))
    println("____________")
    println(ControlWebPage.jobInfoCache)
  }

  def testIncyRedis() = {
    val redis = new CrawlerTaskTraceListener(new CrawlerConf()).redisOps
    println(redis.incrBy("job_test_id", 20))
    println(redis.setValue("job_test_id", 0))
    println(redis.incrBy("job_test_id", 0))

  }
}
