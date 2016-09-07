package com.foofv.crawler.downloader.stream

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.local.ResObjQueue
import com.foofv.crawler.util.Logging

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class LocalTaskStreamProcess private(conf: CrawlerConf) extends TaskStreamProcess with Logging {

  override def persist(resObj: ResObj): Boolean = {
    val result = ResObjQueue.resObjQueue.offer(resObj)
    logInfo(s"push resObj to queue successfully! resObj:${resObj.toString}")
    result
  }
}

private[crawler] object LocalTaskStreamProcess {
  var localTaskStreamProcess: LocalTaskStreamProcess = null
  private val lock = new Object()

  def apply(conf: CrawlerConf): LocalTaskStreamProcess = {
    if (localTaskStreamProcess == null) {
      lock.synchronized {
        if (localTaskStreamProcess == null) {
          localTaskStreamProcess = new LocalTaskStreamProcess(conf)
        }
      }
    }
    localTaskStreamProcess
  }
}