package com.foofv.crawler.local

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.blockqueue.CrawlerBlockQueue
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.parse.Parser
import com.foofv.crawler.parse.worker.ParserWorker

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class DataEntityPersistLocal(conf: CrawlerConf) {
  val parserAndObjAsynCacheQueue = CrawlerBlockQueue[(Parser, ResObj, AnyRef)]("linkedBlock", "asynParserObj", Int.MaxValue, conf)
  init

  def init() = {
    new Thread(new Runnable {
      override def run(): Unit = {
        ParserWorker.parserAndPersistObj(conf)
      }
    }).start()

  }

}
