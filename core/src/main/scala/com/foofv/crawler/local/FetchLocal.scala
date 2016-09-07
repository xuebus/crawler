package com.foofv.crawler.local

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.agent.AgentWorker
import com.foofv.crawler.util.Logging

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class FetchLocal(conf: CrawlerConf) extends Logging{

  init

  def init() = {
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          val taskEntity = TaskEntityQueue.taskEntityPriorityQueue.take()

          //start fetch
          AgentWorker.handleEntity(taskEntity, conf)
          log.info(s"start fetch process,taskEntity:${taskEntity.toString()}")
        }
      }
    }).start()

  }

}
