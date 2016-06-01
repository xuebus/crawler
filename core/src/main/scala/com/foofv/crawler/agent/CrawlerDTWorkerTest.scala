package com.foofv.crawler.agent

import com.google.common.cache._
import java.util.concurrent.TimeUnit
import com.foofv.crawler.util._
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.schedule.{ ITaskQueue, RedisSortedSetTaskQueueImpl, RedisListTaskQueueImpl }
import com.foofv.crawler.enumeration._
import com.foofv.crawler.util.constant.Constant

private[crawler] abstract class CrawlerDTWorkerTest(conf: CrawlerConf,
                                                name: String,
                                                var interval: Long = -1,
                                                var workerType: CrawlerDTWorkerType.Type = CrawlerDTWorkerType.REPEAT) extends Logging {
  // initialize DTWork
  val workerName: String = "DTWorker-" + name
  val workerDaemonName: String = "DTWorker-daemon-" + name
  if (interval == -1) {
    interval = Constant(conf).CRAWLER_DTWORKER_DEFAULT_WORK_INTERVAL
  }
  private val checkInterval: Long = (interval * 5).toLong
  private val expiredTime: Long = interval * 20
  private val clock = new SystemTimerClock()
  var workerThread: Timing = new Timing(clock, interval, doWork, workerName)
  var workerDaemonThread: Timing = new Timing(clock, checkInterval, monitorWorker, workerDaemonName)
  private var runState: Boolean = false

  protected def callback: Long = { -1 }

  var cacheManager = CrawlerDTWorkerTest.initCache(workerName, expiredTime)
  logInfo(workerName + " init finished")

  protected def updateCache(key: String, value: Long) {
    cacheManager.put(key, value)
    val nowStr = Util.convertDateFormat(value)
    logDebug("update cache " + key + "->" + nowStr)
  }

  def updateWorderThreadCache() {
    updateCache(workerName, System.currentTimeMillis())
  }

  def startUp() {
    cacheManager.apply(workerDaemonName)
    workerDaemonThread.start()
    workerThread.start()
    cacheManager.apply(workerName)
    //Thread.sleep(Integer.MAX_VALUE)
  }

  //get workerThread default interval time
  protected def getDefaultIntervaltime = interval

  protected def doWork() {
    monitorWorkerDaemon()
    if (workerType == CrawlerDTWorkerType.NO_REPEAT) {
      if (runState == false) {
        callback
        runState = true
      }
    } else {
      var newPeriod = callback
      if (newPeriod > 0) {
        newPeriod = List(newPeriod, expiredTime - 10).min
        workerThread.period = newPeriod
        logDebug("reset " + workerName + " newPeriod " + newPeriod)
      }
    }
  }

  def monitorWorkerDaemon() {
    val now = System.currentTimeMillis()
    updateCache(workerName, now)
    logDebug("check " + workerDaemonName + " running state")
    val lastTimeCache = Option(cacheManager.getIfPresent(workerDaemonName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerDaemonName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        logInfo(workerDaemonName + " may be dead")
        workerDaemonThread.stop(true)
        workerDaemonThread.restart()
        updateCache(workerDaemonName, now)
      }
    }
  }

  protected def monitorWorker() {
    val now = System.currentTimeMillis()
    updateCache(workerDaemonName, now)
    logDebug("check " + workerName + " running state")

    val lastTimeCache = Option(cacheManager.getIfPresent(workerName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        if (needCheckWorkerCache == false) {
          return
        } else {
        	logInfo(workerName + " may be dead")
        	workerThread.stop(true)
        	runState = false
        	workerThread.restart()
        	updateCache(workerName, now)
        }
      }
    }
  }

  protected def needCheckWorkerCache = true

}

private[crawler] object CrawlerDTWorkerTest {
  def initCache(workerName: String, expiredTime: Long): LoadingCache[java.lang.String, java.lang.Long] = {
    val cacheLoader: CacheLoader[java.lang.String, java.lang.Long] =
      new CacheLoader[java.lang.String, java.lang.Long]() {
        def load(key: java.lang.String): java.lang.Long = {
          long2Long(System.currentTimeMillis())
        }
      }
    var cacheManager = CacheBuilder.newBuilder()
      .expireAfterWrite(expiredTime, TimeUnit.MILLISECONDS).build(cacheLoader)
    cacheManager
  }
}

