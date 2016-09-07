package com.foofv.crawler.local

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.parse.worker.ParserWorker

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class ParseProcessLocal(conf: CrawlerConf) {

  init

  def init() = {
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          val resObj = ResObjQueue.resObjQueue.take()
          ParserWorker.handleMsgLocal(resObj, conf)
        }
      }
    }).start()

  }

}
