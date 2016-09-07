package com.foofv.crawler.downloader.stream

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.agent.AgentWorker
import com.foofv.crawler.entity.{CrawlerTaskEntity, ResObj}
import com.foofv.crawler.queue.{IMessageQueueFactory, MessageQueue}
import com.foofv.crawler.storage.{StorageManager, StorageManagerFactory}
import com.foofv.crawler.util.Logging

/**
  * Created by soledede.weng on 2016/9/7.
  */
private[crawler] class KVTaskStreamProcess private (conf: CrawlerConf) extends TaskStreamProcess with StorageManagerFactory with IMessageQueueFactory  with Logging{


  override def createMessageQueue: MessageQueue = {
    MessageQueue("kafka", conf)
  }

  override def createStorageManager(): StorageManager = {
    StorageManager("oss", conf)
  }

  override def persist(resObj: ResObj): Boolean = {
    var success = false
    val taskEntity: CrawlerTaskEntity = resObj.tastEntity
    try {
      val storageManager = createStorageManager()
      val separator = conf.get("crawler.redis.complexkey.separator", "_").trim()
      val key = (taskEntity.taskId + separator + taskEntity.jobId + separator + taskEntity.currentBatchId).trim
      logDebug(s"persist ResObj [$key]")
      val isSaved = storageManager.putByKey(key, resObj)
      if (isSaved) {
        try {
          product(key)
          success = true
        } catch {
          case e: Throwable => {
            storageManager.delByKey(key)
            logError("push key to Kafka failed! ", e)
          }
        }
      } else {
        logError("persist ResObj failed, task[" + taskEntity.taskId + "]")
      }
    } catch {
      case e: Throwable => {
        logError("persist CrawlerTaskEntity failed" + taskEntity)
        logError("persist ResObj failed! ", e)
      }
    } finally {
      if (success == false) {
        AgentWorker.putFailedTaskBacktoSortedSet(taskEntity)
      }
    }
    success
  }

  def product(identity: String) = {
    val msgQueue = createMessageQueue
    msgQueue.sendMsg(identity)
    logInfo("push key [" + identity + "] to Kafka")
    // test
    //val msg = msgQueue.consumeMsg()
    //logInfo(s"consume msg key [$msg]")
  }
}



private[crawler] object KVTaskStreamProcess {
  var kvTaskStreamProcess: KVTaskStreamProcess = null
  private val lock = new Object()

  def apply(conf: CrawlerConf): KVTaskStreamProcess = {
    if (kvTaskStreamProcess == null) {
      lock.synchronized {
        if (kvTaskStreamProcess == null) {
          kvTaskStreamProcess = new KVTaskStreamProcess(conf)
        }
      }
    }
    kvTaskStreamProcess
  }
}


