package com.foofv.crawler.agent

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.schedule.{ITaskQueue, RedisSortedSetTaskQueueImpl, RedisListTaskQueueImpl}
import com.foofv.crawler.enumeration._
import com.foofv.crawler.control._
import com.foofv.crawler.blockqueue.ICrawlerBlockQueueFactory
import com.foofv.crawler.blockqueue.CrawlerBlockQueue
import com.foofv.crawler.blockqueue.CrawlerArrayBlockQueueImpl
import com.foofv.crawler.blockqueue.CrawlerLinkedBlockQueueImpl
import com.foofv.crawler.enumeration.CrawlerDTWorkerType

class CrawlerDTScheduleTask(conf: CrawlerConf,
                            name: String = "ScheduleTask",
                            var intervalMillionSec: Long = -1,
                            scheduleTask: () => Long,
                            checkWorkerCacheCondition: () => Boolean = () => true)
  extends  CrawlerDTWorkerTest(conf, name, interval = intervalMillionSec, workerType = CrawlerDTWorkerType.NO_REPEAT) {

  //  override var interval: Long = conf.getLong(intervalKey, 10) * 1000

  override def callback = {
    scheduleTask()
  }

  override def needCheckWorkerCache = {
    checkWorkerCacheCondition()
  }
}

object CrawlerDTScheduleTask {
  var taskCounter = 0
}

object TestCrawlerDTScheduleTask {

  def main(args: Array[String]): Unit = {
    testCrawlerDTScheduleTask()
  }

  def testCrawlerDTScheduleTask() {
    val t = new CrawlerDTScheduleTask(new CrawlerConf(), scheduleTask = () => {
      -1L
    }, checkWorkerCacheCondition = () => {
      true
    })
    t.startUp()
  }
}

