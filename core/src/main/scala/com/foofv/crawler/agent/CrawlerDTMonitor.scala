package com.foofv.crawler.agent

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.control._
import com.foofv.crawler.enumeration.CrawlerDTWorkerType

import com.foofv.crawler.util._

class CrawlerDTMonitor(conf: CrawlerConf,
                           name: String = "Monitor",
                           crawlerDTWorker: CrawlerDTWorkerTest)
  extends CrawlerDTWorkerTest(conf, name){
  
  override def callback = { 
    val now = System.currentTimeMillis()
    val workerDaemonName = crawlerDTWorker.workerDaemonName
    logDebug("check " + workerDaemonName + " running state")
    val lastTimeCache = Option(crawlerDTWorker.cacheManager.getIfPresent(workerDaemonName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerDaemonName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        logInfo(workerDaemonName + " may be dead")
        crawlerDTWorker.workerDaemonThread.stop(true)
        crawlerDTWorker.workerDaemonThread.restart()
        updateCache(workerDaemonName, now)
      }
    }
    -1
  }
  
}

object TestCrawlerDTMonitor {
  
  def main(args: Array[String]): Unit = {
		  CrawlerDTMonitor()
  }
  
  def CrawlerDTMonitor(){
	  //val t = new AgentMasterDTMonitor(new CrawlerConf(), doMonitor = () => -1)
	  //t.startUp()
  }
}
