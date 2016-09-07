package com.foofv.crawler.downloader.stream

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.ResObj

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] trait TaskStreamProcess {

  def persist(resObj: ResObj): Boolean

}

private[crawler] object TaskStreamProcess {
  def apply(conf: CrawlerConf, streamType: String = "kv"): TaskStreamProcess = {
    streamType match {
      case "kv" => KVTaskStreamProcess(conf)
      case "local" => LocalTaskStreamProcess(conf)
      case _ => null
    }
  }
}

private[crawler] trait TaskStreamProcessFactory {
  def createTaskStreamProcess(): TaskStreamProcess
  def createLocalTaskStreamProcess(): TaskStreamProcess
}