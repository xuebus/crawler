package com.foofv.crawler.blockqueue

import com.foofv.crawler.util.Logging
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import com.foofv.crawler.entity.CrawlerTaskEntity

/**
 * The Blocking Queue of Array,it need a array size
 * @author soledede
 */

private[crawler] object CrawlerArrayBlockQueueImpl extends Logging {

  private val count = new AtomicInteger(0)

  private var coun1 = 0;

  private val blockSize = 1000; // you can get size from config 
  private val taskBlockingQueue = new ArrayBlockingQueue[CrawlerTaskEntity](blockSize)

   def put(task: CrawlerTaskEntity): Unit = {
    this.taskBlockingQueue.put(task)
  }

   def take(): CrawlerTaskEntity = {
    this.taskBlockingQueue.take()
  }

   def size(): Int = {
    this.taskBlockingQueue.size()
  }

  def test() {
    /*count.getAndIncrement
    coun1 =1 + coun1*/

    for (i <- 1 to 10) {
      coun1 = 1 + coun1

      count.getAndIncrement
      logInfo(count.getAndIncrement + "log")
    }

    println(coun1 + "ddd" + Thread.currentThread().getName + " 进来了！" + count.get)
    /* for(i <- 1 to 10){
   coun1 =1 + coun1
   }
   count.getAndIncrement*/

  }

}

object testCrawlerQueue {

  def main(args: Array[String]): Unit = {
    /* for(i <- 1 to 10){
      CrawlerArrayBlockQueueImpl.test()
    }*/

    for (i <- 1 to 5) {
      Thread.sleep(1000L)
      new Thread() {
        override def run() {
          //CrawlerArrayBlockQueueImpl.test()
        }
      }.start()
    }
  }
}
  
