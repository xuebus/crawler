package com.foofv.crawler.schedule

import com.foofv.crawler.entity.CrawlerTaskEntity

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] trait TaskManager {

  def submitTask[T](task: T): Boolean = false
  def submitTask(task: CrawlerTaskEntity,score: Long): Boolean
}
