package com.foofv.crawler.agent

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.schedule.{ ITaskQueue, RedisSortedSetTaskQueueImpl, RedisListTaskQueueImpl }
import com.foofv.crawler.enumeration._
import com.foofv.crawler.control._
import com.foofv.crawler.blockqueue.ICrawlerBlockQueueFactory
import com.foofv.crawler.blockqueue.CrawlerBlockQueue
import com.foofv.crawler.blockqueue.CrawlerArrayBlockQueueImpl
import com.foofv.crawler.blockqueue.CrawlerLinkedBlockQueueImpl
import com.foofv.crawler.util.constant.Constant

//get CrawlerTaskEntity from Redis.list
//put CrawlerTaskEntity to CrawlerBlockQueue
class CrawlerDTPutTaskToBlockQueue(conf: CrawlerConf,
                           name: String = "PutTaskToBlockQueue")
  extends CrawlerDTWorkerTest(conf, name) with ICrawlerBlockQueueFactory{

  // initialize
  private val taskList = ITaskQueue("list",conf)
  private val taskListKey = Constant(conf).CRAWLER_TASK_LIST_KEY

  override def callback = {
    var newPeriod =  this.getDefaultIntervaltime
    logDebug("PutTaskToBlockQueue START")
    val result = taskList.getLastElement[CrawlerTaskEntity](taskListKey)
    if (result == null) {
      logDebug("task List is EMPTY!")
      //val offsetTime = this.getDefaultIntervaltime * (util.Random.nextInt(CrawlerDTPutTaskToBlockQueue.ScanIntervalTimes)+1)
      //logDebug("offsetTime "+offsetTime)
      //newPeriod = offsetTime
    } else {
      val taskQueue  = getCrawlerBlockQueue[CrawlerTaskEntity]
      taskQueue.put(result)
      logDebug("put task ["+result.taskId+"] to CrawlerBlockQueue")
      logDebug("CrawlerBlockQueue size["+taskQueue.size()+"]")
      newPeriod = 1
    }
    logDebug("PutTaskToBlockQueue END")
    newPeriod
  }
  
  def getCrawlerBlockQueue[T]: CrawlerBlockQueue[T] = {
    CrawlerBlockQueue[T]("linkedBlock","getTask",conf)
  }

}

object TestCrawlerDTPutTaskToBlockQueue {
  
  def main(args: Array[String]): Unit = {
		  testAgentMasterTaskDTScanner()
  }
  
  def testRedisList(){
    val conf = new CrawlerConf
    val taskList = ITaskQueue("list",conf)
    val t = taskList.getFirstElement[String]("qqqq")
    System.err.println(t)
  }
  
  def testAgentMasterTaskDTScanner(){
	  val t = new CrawlerDTPutTaskToBlockQueue(new CrawlerConf())
	  t.startUp()
  }
}

