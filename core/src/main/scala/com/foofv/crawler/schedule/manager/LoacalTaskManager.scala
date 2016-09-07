package com.foofv.crawler.schedule.manager

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.local.TaskEntityQueue
import com.foofv.crawler.schedule.TaskManager

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class LoacalTaskManager(conf: CrawlerConf) extends TaskManager {
  override def submitTask(task: CrawlerTaskEntity, score: Long): Boolean = {
    task.taskStartTime = score
    //submit task to local queue
    TaskEntityQueue.taskEntityPriorityQueue.offer(task)
  }
}

object LoacalTaskManager {

}
