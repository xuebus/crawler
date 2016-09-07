package com.foofv.crawler.local

import java.util.Comparator
import java.util.concurrent.{LinkedBlockingQueue, PriorityBlockingQueue}

import com.foofv.crawler.entity.{CrawlerTaskEntity, ResObj}

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] object TaskEntityQueue {
  val taskEntityPriorityQueue = new PriorityBlockingQueue[CrawlerTaskEntity](1000, new Comparator[CrawlerTaskEntity] {
    override def compare(c1: CrawlerTaskEntity, c2: CrawlerTaskEntity): Int = (c1.taskStartTime - c2.taskStartTime).toInt
  })
}

private[crawler] object ResObjQueue {
  val resObjQueue = new LinkedBlockingQueue[ResObj](1000)
}
