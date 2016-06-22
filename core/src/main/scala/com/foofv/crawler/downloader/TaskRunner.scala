package com.foofv.crawler.downloader

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.control.CrawlerControlImpl
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.util.Logging

/**
 * TaskRunner for fetch
 * We need add it to thread pool
 * @author soledede
 */
private[crawler]
class TaskRunner(taskEntity: CrawlerTaskEntity, downlaoder: Downloader) extends Runnable with Logging {

  override def run() {
    try {
      downlaoder.asynfetch(taskEntity)
    } catch {
      case t: Throwable => logError("TaskRunner Error", t)
    }
  }

}

object Main extends App {

  val downloader = Downloader("http", new CrawlerConf())
  val taskEntity = new CrawlerTaskEntity()
//  taskEntity.httpRefer = "http://developer.baidu.com/map/wiki/index.php?title=car/api/geocoding"
//  taskEntity.userAgent="Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0"
  taskEntity.taskURI = "http://api.map.baidu.com/telematics/v3/reverseGeocoding?location=12960622.94,4843926.42&coord_type=bd09mc&ak=AOkR59MT4atImiOo3BGee0lL&output=json"
  //  new Thread(new TaskRunner(taskEntity, downloader))
  downloader.asynfetch(taskEntity)
}