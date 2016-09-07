package com.foofv.crawler.storage

import com.foofv.crawler.CrawlerConf

import scala.reflect.ClassTag

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class LocalStorageManager private (conf: CrawlerConf) extends StorageManager{
  override def put[T: ClassTag](entity: T): Boolean = {
    println(entity.toString)
    false
  }

}
object LocalStorageManager extends App {

  var singleton: LocalStorageManager = null
  val lock = new Object()

  def apply(conf: CrawlerConf): LocalStorageManager = {
    if (singleton == null) {
      lock.synchronized {
        if (singleton == null) {
          singleton = new LocalStorageManager(conf)
        }
      }
    }
    singleton
  }
}