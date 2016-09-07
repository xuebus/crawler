package com.foofv.crawler.schedule.manager

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.schedule.TaskManager
import com.foofv.crawler.util.constant.Constant

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class RedisTaskManager(conf: CrawlerConf) extends TaskManager {
  override def submitTask(task: CrawlerTaskEntity,score: Long): Boolean = {
    val f = conf.taskQueue.putElementToSortedSet(Constant(conf).CRAWLER_TASK_SORTEDSET_KEY, task, score)
    f
  }
}
