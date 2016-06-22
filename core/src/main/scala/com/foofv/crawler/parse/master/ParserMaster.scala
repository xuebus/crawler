package com.foofv.crawler.parse.master

import com.foofv.crawler.resource.master.Master
import com.foofv.crawler.CrawlerException
import com.foofv.crawler.util.AkkaUtil
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Logging
import akka.actor.Props
import akka.actor.ActorSystem
import com.foofv.crawler.queue.MessageQueue
import com.foofv.crawler.agent.CrawlerDTWorker
import scala.util.control.Breaks._
import com.foofv.crawler.deploy.TransferMsg.TaskKeyMsg
import com.foofv.crawler.agent.CrawlerDTMonitor
import com.foofv.crawler.agent.CrawlerDTScheduleTask
import com.foofv.crawler.util._
import com.foofv.crawler.util.constant.Constant

/**
 * Schedule resource 、 tast 。。。
 * @author soledede
 */
private[crawler] class ParserMaster(
                                     override val host: String,
                                     override val port: Int,
                                     conf: CrawlerConf)
  extends Master(host, port, conf) {

  val iQueue = MessageQueue("kafka", conf)
  var consumeMsgDTTask: CrawlerDTWorker = null
  var consumeMsgDTMonitor: CrawlerDTWorker = null

  /**
   * override initMaster from Master
   * get task key and allocate it to parser worker from queue(we use kafka as blocking queue)
   * if no enough resource,you need sleep 3s,then put it in blocking queue
   */
  override def initMaster() = {
    iQueue.start()
    val interval = Constant(conf).CRAWLER_DTWORKER_DEFAULT_WORK_INTERVAL
    consumeMsgDTTask = new CrawlerDTWorker(name = "consumeMsgWorkerTask", interval = interval, callback = consumeMsg _, isRestartIfTimeout = checkWorkerCacheCondition _)
    consumeMsgDTTask.startUp()
    
    consumeMsgDTMonitor = new CrawlerDTWorker(name = "consumeMsgDTMonitor", interval = interval, callback = minotorConsumeMsgDTTask _)
    consumeMsgDTMonitor.startUp()

    logInfo("ParserMaster start up")
  }
  
  private def minotorConsumeMsgDTTask = {
    val now = System.currentTimeMillis()
    val workerDaemonName = consumeMsgDTTask.workerDaemonName
    logDebug("check " + workerDaemonName + " running state")
    val lastTimeCache = Option(consumeMsgDTTask.cacheManager.getIfPresent(workerDaemonName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerDaemonName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        logInfo(workerDaemonName + " may be dead")
        consumeMsgDTTask.workerDaemonThread.stop(true)
        consumeMsgDTTask.workerDaemonThread.restart()
        consumeMsgDTMonitor.updateCache(workerDaemonName, now)
      }
    }
    -1
  }

  private def checkWorkerCacheCondition(): Boolean = {
    if (iQueue.size() == 0) {
      logInfo("stat blocked queue size [" + iQueue.size() + "]")
      false
    } else {
      true
    }
  }

  private def consumeMsg(): Long = {
    while (true) {
      val msg = iQueue.consumeMsg()
      logInfo(s"consume msg[$msg]")
      consumeMsgDTTask.updateWorderThreadCache()
      var cnt = 0
      var refreshInval = 0
      breakable {
        while (true) {
          refreshInval += 1
          if (refreshInval == 10) {
            refreshInval = 0
            resAllocator.refreshResList
          }
          val workerId = resAllocator.availableWorkerId(this)
          if (workerId == null) {
            resAllocator.refreshResList
            println("have no available resource,sleep %d s...".format(3))
            Thread.sleep(3000)
            logInfo("put task id into bloking queue again!")
            iQueue.sendMsgLocal(msg)
            break
          } else {
            //allocate task key
            if (idToWorker.contains(workerId)) {
              cnt = 0
              val workerInf = idToWorker(workerId)
              workerInf.actor ! TaskKeyMsg(msg)
              break
            } else {
              Thread.sleep(3000)
              cnt += 1
              if (cnt >= 5) logWarning("I have retry find parserworker " + cnt + "times, all parser worker may be dead!")
            }
          }
        }
      }
    }
    -1
  }

}

private[crawler] object ParserMaster {

  val systemName = "ParserMasterSys"
  private val actorName = "ParserMaster"
  val crawlerUrlRegex = "crawler://([^:]+):([0-9]+)".r

  def main(argStrings: Array[String]): Unit = {
    val conf = new CrawlerConf
    val args = new MasterArguments(argStrings, conf)
    //args.host = "192.168.10.146"
    //        args.host = "192.168.10.47"
    //    args.port = 9998
    //args.host = "192.168.10.25"
    //args.port = 9998

    val (actorSystem, _) = startSystemAndActor(args.host, args.port, conf)
    actorSystem.awaitTermination()
  }

  /** Returns an `akka.tcp://...` URL for the Master actor given a crawlerUrl `crawler://host:ip`. */
  def toAkkaUrl(crawlerUrl: String): String = {
    crawlerUrl match {
      case crawlerUrlRegex(host, port) =>
        "akka.tcp://%s@%s:%s/user/%s".format(systemName, host, port, actorName)
      case _ =>
        throw new CrawlerException("Invalid parserMaster URL: " + crawlerUrl)
    }
  }

  def startSystemAndActor(
                           host: String,
                           port: Int,
                           conf: CrawlerConf): (ActorSystem, Int) = {
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    val actor = actorSystem.actorOf(Props(classOf[ParserMaster], host, boundPort, conf), actorName)
    (actorSystem, boundPort)
  }
}