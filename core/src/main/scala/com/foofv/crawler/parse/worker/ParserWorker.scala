package com.foofv.crawler.parse.worker

import com.foofv.crawler.control.CrawlerControlManager
import com.foofv.crawler.parse.scheme.nodes.SemanticNode
import com.foofv.crawler.parse.topic.SchemeTopicParser
import com.foofv.crawler.resource.slave.Worker
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.CrawlerException
import akka.actor.Address
import com.foofv.crawler.util.AkkaUtil
import akka.actor.Props
import com.foofv.crawler.util.Logging
import akka.actor.ActorSystem
import com.foofv.crawler.deploy.TransferMsg._
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.parse.master.ParserMaster
import com.foofv.crawler.storage.StorageManagerFactory
import com.foofv.crawler.storage.StorageManager
import com.foofv.crawler.entity.ResObj
import org.apache.http.HttpResponse
import com.foofv.crawler.util.Util
import com.foofv.crawler.parse.ParserRunner
import com.foofv.crawler.agent.CrawlerDTMonitor
import com.foofv.crawler.agent.CrawlerDTScheduleTask
import com.foofv.crawler.agent.CrawlerDTWorker
import com.foofv.crawler.blockqueue.CrawlerBlockQueue
import com.foofv.crawler.parse.Parser
import com.foofv.crawler.parse.ParserPersistRunner

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Await
import java.net.SocketTimeoutException
import java.util.concurrent.ThreadPoolExecutor

/**
  * do work
  *
  * @author soledede
  */
private[crawler] class ParserWorker(
                                     override val host: String,
                                     override val port: Int,
                                     override val cores: Int,
                                     override val memory: Int,
                                     override val masterUrls: Array[String],
                                     override val actorSystemName: String,
                                     override val actorName: String,
                                     override val conf: CrawlerConf,
                                     override val controlUrl: String) extends Worker(host, port, cores, memory, masterUrls, actorSystemName, actorName, conf, controlUrl) with Logging with StorageManagerFactory {

  var storageManager: StorageManager = createStorageManager
  var parserThreadPool = Util.newDaemonFixedThreadPool(conf.getInt("crawler.parser.worker.threadnum", Util.inferCores() * conf.getInt("cralwer.parser.worker.core.thread", 2)), "parser_thread_excutor")
  var asynPersistThreadPool = Util.newDaemonFixedThreadPool(conf.getInt("crawler.parser.worker.threadnum", Util.inferCores() * conf.getInt("cralwer.parser.worker.core.thread", 2)), "asynPersist_thread_excutor")
  val parserAndObjAsynCacheQueue = CrawlerBlockQueue[(Parser, ResObj, AnyRef)]("linkedBlock", "asynParserObj", Int.MaxValue, conf)
  val rowKeyFailedTmpCache = CrawlerBlockQueue[String]("linkedBlock", "rowKeyFailedTmpCacheObj", Int.MaxValue, conf)
  var tmpCountRowKeyDump: Int = 0

  var parserAndPersistObjDTTask: CrawlerDTWorker = null
  var parserAndPersistObjDTMonitor: CrawlerDTWorker = null

  override def intiWorker() = {
    implicit val timeout = akka.util.Timeout.apply(5, java.util.concurrent.TimeUnit.SECONDS)
    val controlSelect = context.actorSelection(CrawlerControlManager.toAkkaUrl(controlUrl)).resolveOne()
    try {
      val result = Await.result(controlSelect, timeout.duration)
    } catch {
      case e => logWarning("init Worker timeout!", e)
    }
    SchemeTopicParser.controlActor = controlSelect.value.get.get

    // SchemeTopicParser.controlActor ! JobId("1234567890")
    parserAndPersistObjDTTask = new CrawlerDTWorker(name = "parserAndPersistObjWorkerTask", callback = parserAndPersistObj _, isRestartIfTimeout = checkWorkerCacheCondition _)
    parserAndPersistObjDTTask.startUp()

    parserAndPersistObjDTMonitor = new CrawlerDTWorker(name = "parserAndPersistObjDTMonitor", callback = minotorParserAndPersistObjDTTask _)
    parserAndPersistObjDTMonitor.startUp()
    logInfo("ParserWorker start up")

  }

  private def minotorParserAndPersistObjDTTask = {
    val now = System.currentTimeMillis()
    val workerDaemonName = parserAndPersistObjDTTask.workerDaemonName
    logDebug("check " + workerDaemonName + " running state")
    val lastTimeCache = Option(parserAndPersistObjDTTask.cacheManager.getIfPresent(workerDaemonName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerDaemonName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        logInfo(workerDaemonName + " may be dead")
        parserAndPersistObjDTTask.workerDaemonThread.stop(true)
        parserAndPersistObjDTTask.workerDaemonThread.restart()
        parserAndPersistObjDTMonitor.updateCache(workerDaemonName, now)
      }
    }
    -1
  }

  private def checkWorkerCacheCondition(): Boolean = {
    if (parserAndObjAsynCacheQueue.size() == 0) {
      logInfo("stat blocked queue size [" + parserAndObjAsynCacheQueue.size() + "]")
      false
    } else {
      true
    }
  }

  private def parserAndPersistObj(): Long = {
    logInfo("parser and persist ResObj  starting...")
    while (true) {
      val paserResObj = parserAndObjAsynCacheQueue.take()
      parserAndPersistObjDTTask.updateWorderThreadCache()
      if (paserResObj != null)
        submitPaserResObjThreadPool(paserResObj._1, paserResObj._2, paserResObj._3)
    }
    -1
  }

  def submitPaserResObjThreadPool(parser: Parser, resObj: ResObj, obj: AnyRef) = {
    if (parser != null && resObj != null)
      asynPersistThreadPool.submit(new ParserPersistRunner(parser, resObj, obj, conf))
  }

  override def changeMaster(url: String) = {
    activeMasterUrl = url
    master = context.actorSelection(ParserMaster.toAkkaUrl(activeMasterUrl))
    masterAddress = activeMasterUrl match {
      case ParserMaster.crawlerUrlRegex(_host, _port) =>
        Address("akka.tcp", ParserMaster.systemName, _host, _port.toInt)
      case x =>
        throw new CrawlerException("Invalid crawler URL: " + x)
    }
    registrationRetryTimer.foreach(_.cancel())
    registrationRetryTimer = None
    connected = true

  }

  override def registerAllMasters() {
    for (masterUrl <- masterUrls) {
      logInfo("Connecting to master " + masterUrl + "...")
      val actor = context.actorSelection(ParserMaster.toAkkaUrl(masterUrl))
      actor ! RegisterWorker(workerId, host, port, cores, memory)
    }
  }

  //get ResObj from storage by key from queue and put it into threadPool
  override def handleMsg(msg: String) = {
    //asynResObj(msg)
    logInfo(s"handleMsg [$msg]")
    val resObj = resResObj(msg)
    if (resObj != null) parserThreadPool.submit(new ParserRunner(resObj, conf))
    logInfo("rowKeyFailedTmpCache.size" + rowKeyFailedTmpCache.size())
    if (rowKeyFailedTmpCache.size() > 0) {
      rowKeyFailedTmpCache.toArray().foreach {
        x => {
          rowKeyFailedTmpCache.remove(x.asInstanceOf[String])
          val ro = resResObj(x.asInstanceOf[String])
          if (ro != null) {
            parserThreadPool.submit(new ParserRunner(resObj, conf))
          }
        }
      }
    }
  }


  private def resResObj(rowkey: String) = {
    var resObj: ResObj = null
    try {
      resObj = storageManager.getByKeyWithException[ResObj](rowkey)
      tmpCountRowKeyDump = 0
      storageManager.delByKey(rowkey)
    } catch {
      case e: java.net.SocketTimeoutException => {
        logError(s"get object from cloud storage by key[$rowkey] failed: SocketTimeoutException", e)
        tmpCountRowKeyDump += 1
        rowKeyFailedTmpCache.offer(rowkey)
        logError("rowKeyFailedTmpCache.size" + rowKeyFailedTmpCache.size())
        if (tmpCountRowKeyDump == 5) Thread.sleep(4 * 1000)
      }
      case e: Exception => {
        logError(s"get object from cloud storage by key[$rowkey] failed!", e)
      }
    }
    resObj
  }

  //1
  private def asynResObj(rowkey: String) = {
    storageManager.asynByKey(rowkey, callbackResponse) //get storage manager
  }

  private def callbackResponse(response: HttpResponse) = {
    // DON'T Need DO IT
    //get ResObj from HttpResponse 
    //put ResObj into BlockingQueue, put don't need block
  }

  private def delResObj(rowkey: String): Boolean = {
    //TODO
    //delete resObj from storage
    false
  }

  override def createStorageManager(): StorageManager = {
    StorageManager("oss", conf)
  }

  override def handleScheme(jobId: String, tree: mutable.Map[String, SemanticNode]) = {
    SchemeTopicParser.schemeDocCacheManager.put(jobId, tree)
  }
}

private[crawler] object ParserWorker extends Logging {
  var parserThreadPool: ThreadPoolExecutor = null
  var asynPersistThreadPool: ThreadPoolExecutor = null

  def main(argStrings: Array[String]): Unit = {


    val conf = new CrawlerConf
    val args = new WorkerArguments(argStrings, conf)
    //args.masters(0) = "crawler://192.168.10.47:9998"
    //args.masters(0) = "crawler://192.168.10.25:9998"
    //args.masters(0) = "crawler://192.168.10.146:9998"

    //args.controlUrl = "control://192.168.10.25:9997"
    val (actorSystem, _) = startSystemAndActor(args.host, args.port, args.cores, args.memory, args.masters, args.controlUrl)
    actorSystem.awaitTermination()
  }

  def startSystemAndActor(
                           host: String,
                           port: Int,
                           cores: Int,
                           memory: Int,
                           masterUrls: Array[String],
                           controlUrl: String,
                           workerNumber: Option[Int] = None): (ActorSystem, Int) = {

    val conf = new CrawlerConf
    val systemName = "parserWorkerSys" + workerNumber.map(_.toString).getOrElse("")
    val actorName = "ParserWorker"
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    actorSystem.actorOf(Props(classOf[ParserWorker], host, boundPort, cores, memory,
      masterUrls, systemName, actorName, conf, controlUrl), name = actorName)
    (actorSystem, boundPort)
  }

  def handleMsgLocal(resObj: ResObj, conf: CrawlerConf) = {
    if (parserThreadPool == null) {
      parserThreadPool = Util.newDaemonFixedThreadPool(conf.getInt("crawler.parser.worker.threadnum", Util.inferCores() * conf.getInt("cralwer.parser.worker.core.thread", 2)), "parser_thread_excutor")
    }
    if (resObj != null) {
      parserThreadPool.submit(new ParserRunner(resObj, conf))
      logInfo(s"receive resObj for parse thread pool by local,resObj:${resObj.toString}")
    }

  }

  //process the entity that have been parsed
  def parserAndPersistObj(conf: CrawlerConf) = {
    while (true) {
      val paserResObj = conf.dataEntityPersistLocal.parserAndObjAsynCacheQueue.take()
      if (paserResObj != null)
        submitPaserResObjThreadPool(paserResObj._1, paserResObj._2, paserResObj._3, conf)
    }
  }

  def submitPaserResObjThreadPool(parser: Parser, resObj: ResObj, obj: AnyRef, conf: CrawlerConf) = {
    if (asynPersistThreadPool == null)
      asynPersistThreadPool = Util.newDaemonFixedThreadPool(conf.getInt("crawler.parser.worker.threadnum", Util.inferCores() * conf.getInt("cralwer.parser.worker.core.thread", 2)), "asynPersist_thread_excutor")
    if (parser != null && resObj != null)
      asynPersistThreadPool.submit(new ParserPersistRunner(parser, resObj, obj, conf))
  }

}
