package com.foofv.crawler.local

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.agent.AgentWorker
import com.foofv.crawler.util.Logging

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class FetchLocal(conf: CrawlerConf) extends Logging{
  private val random = new java.util.Random()
  init

  def init() = {
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          val taskEntityQueue = TaskEntityQueue.taskEntityPriorityQueue
          val taskEntity = taskEntityQueue.take()
          //start fetch
            AgentWorker.handleEntity(taskEntity, conf)
            log.info(s"start fetch process,taskEntity:${taskEntity.toString()}")
            var interval =  random.nextInt(60 * 1000*2)
            interval =  1000*60*4+interval
            println(s"sleep:$interval s")
            Thread.sleep(interval)

        }
      }
    }).start()

  }

}
