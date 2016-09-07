package com.foofv.crawler.downloader

import java.io.InputStream
import java.net.InetSocketAddress
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.TimeUnit

import com.foofv.crawler.util.listener.{JobTaskFailed, ManagerListenerWaiter}

import scala.collection.mutable.HashMap
import scala.util.control.Breaks._
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpEntity
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.util.EntityUtils
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.agent.AgentWorker
import com.foofv.crawler.antispamming.AntiSpamming
import com.foofv.crawler.antispamming.IAntiSpammingFactory
import com.foofv.crawler.antispamming.RedisAntiSpamming
import com.foofv.crawler.downloader.stream.{TaskStreamProcess, TaskStreamProcessFactory}
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.enumeration.CrawlerTaskType
import com.foofv.crawler.queue.IMessageQueueFactory
import com.foofv.crawler.queue.MessageQueue
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.schedule.ITaskQueueFactory
import com.foofv.crawler.storage.StorageManager
import com.foofv.crawler.storage.StorageManagerFactory
import com.foofv.crawler.util.LogOper
import com.foofv.crawler.util.Logging
import com.foofv.crawler.util.constant.Constant
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader

import scala.actors.threadpool.AtomicInteger

/**
  * @author soledede
  */
private[crawler] class HttpDownloader private(conf: CrawlerConf) extends Downloader with ITaskQueueFactory
  with RequestFactory with StorageManagerFactory with IMessageQueueFactory with IAntiSpammingFactory with TaskStreamProcessFactory with Logging with LogOper {

  val w = ManagerListenerWaiter()

  HttpDownloader.refusedCountMax = conf.getInt("crawler.downloader.refused.count.max", 3)
  val redisTaskSortedSet = createTaskQueue()
  val redisTaskSortedSetKey = Constant(conf).CRAWLER_TASK_SORTEDSET_KEY

  private[this] var redis = createAntiSpamming

  def createAntiSpamming: AntiSpamming = {
    new RedisAntiSpamming(conf)
  }

  override def fetch(taskEntity: CrawlerTaskEntity): ResObj = {
    var req = createRequest()
    HttpDownloader.TaskEntityHashMap.put(taskEntity.taskId, taskEntity)
    val resp = req.download(taskEntity)
    HttpDownloader.TaskEntityHashMap.remove(taskEntity.taskId)
    new ResObj(taskEntity, resp)
  }

  override def asynfetch(taskEntity: CrawlerTaskEntity) = {
    try {
      var req = createRequest()
      HttpDownloader.TaskEntityHashMap.put(taskEntity.taskId, taskEntity)
      req.download(taskEntity, callback)
    } catch {
      case t: Throwable => logError("HttpDownloader.asynfetch error", t)
    }
  }

  // persist ResObj to Storage by key
  // if success, send key to Kafka
  override def persist(resObj: ResObj): Boolean = {
    if (!conf.getBoolean("local", false)) {
      createTaskStreamProcess().persist(resObj)
    } else {
      createLocalTaskStreamProcess().persist(resObj)
    }

    /*var success = false
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
    success*/
  }

  //send key to Kafka
  override def product(identity: String) = {
    val msgQueue = createMessageQueue
    msgQueue.sendMsg(identity)
    logInfo("push key [" + identity + "] to Kafka")
    // test
    //val msg = msgQueue.consumeMsg()
    //logInfo(s"consume msg key [$msg]")
  }

  // callback of CloseableHttpAsyncClient.execute
  // when Http request completed, invoked to handle HTTP response and do something else
  var callbackCounter = new AtomicInteger

  def callback(context: HttpClientContext, httpResp: org.apache.http.HttpResponse): Unit = {

    val taskEntity = HttpDownloader.getTaskEntityFromHttpClientContext(context)
    if (taskEntity != null) {
      val taskId = taskEntity.taskId

      val callbackCnt = callbackCounter.getAndIncrement
      var taskNo = ""
      var cnt = context.getAttribute(Constant.CRAWLER_AGENT_WORKER_TASK_NO)
      if (cnt != null) {
        taskNo = cnt.toString()
      }
      logInfo(s"callback invoked taskNo[$taskNo], callbackCounter[$callbackCounter]")

      // get Http request startTime from HttpClientContext
      val startTime = context.getAttribute(HttpDownloader.TaskEntity_Starttime).asInstanceOf[Long]
      val endTime = System.currentTimeMillis()
      val costTime = endTime - startTime
      val ip = taskEntity.taskIp
      val statusCode = httpResp.getStatusLine.getStatusCode
      val domain = taskEntity.taskDomain

      if (statusCode == 404) {
        logTimes(taskId, domain, ip, startTime, endTime, costTime, statusCode)
        w.post(JobTaskFailed(taskEntity.jobId.toString, taskEntity.jobName, 1))
      } else if (statusCode != 200) {
        // if  HTTP StatusCode != 200, HTTP request failed, update refusedState
        var refusedState = false
        var putTaskEntityBacktoRedisSortedSet = true
        val entity = httpResp.getEntity
        var respContent = EntityUtils.toString(entity)

        if (statusCode == 302) {
          val taskId = taskEntity.taskId
          val taskUrl = taskEntity.taskURI
          val location = httpResp.getFirstHeader("location").getValue
          logInfo(s"taskId[$taskId],taskUrl[$taskUrl] HTTP StatusCode 302, redirect location [$location]")
          if (taskEntity.keyWordsOfInvalid != null && !taskEntity.keyWordsOfInvalid.equalsIgnoreCase("null") && location != null && location.contains(taskEntity.keyWordsOfInvalid)) {
            refusedState = false
            putTaskEntityBacktoRedisSortedSet = false
            logInfo(s"taskId[$taskId] taskUrl[$taskUrl] invalid")
            w.post(JobTaskFailed(taskEntity.jobId.toString, taskEntity.jobName, 1))
          } else {
            respContent = handleHttpStatusCode302(taskEntity, httpResp)

            //local,don't process  for forbidden
            if (!conf.getBoolean("local", false))
              forbiddenProcess

            def forbiddenProcess: AnyVal = {
              // Http response content may contain infomation about forbidden
              if (redis.isRefusedMoudle(taskId, domain, ip, costTime, statusCode, refusedState, respContent)) {
                // update and get refusedCount
                val refusedCount = HttpDownloader.updateRefuseCountHashMap(domain)
                if (refusedCount >= HttpDownloader.refusedCountMax) {
                  //if forbidden
                  refusedState = true
                  requestCoutForbidden(taskEntity, domain, true) //request count in some interval
                }
              }
            }

          }
        } else if (taskEntity.forbiddenCode != null && !taskEntity.forbiddenCode.equalsIgnoreCase("null")) {
          if (taskEntity.forbiddenCode.toInt == statusCode) {
            val refusedCount = HttpDownloader.updateRefuseCountHashMap(domain)
            if (refusedCount >= HttpDownloader.refusedCountMax) {
              //if forbidden
              refusedState = true
              //local,don't process  for forbidden
              if (!conf.getBoolean("local", false))
                requestCoutForbidden(taskEntity, domain, true) //request count in some interval
            }
          }
        } else {
          val entity = httpResp.getEntity
          respContent = EntityUtils.toString(entity)
        }

        if (!conf.getBoolean("local", false))
          if (putTaskEntityBacktoRedisSortedSet) {
            AgentWorker.putFailedTaskBacktoSortedSet(taskEntity)
          }
        logTimes(taskId, domain, ip, startTime, endTime, costTime, statusCode, refusedState, content = respContent)
      } else {
        //HTTP request success, get response content and persist ResObj
        logTimes(taskId, domain, ip, startTime, endTime, costTime, statusCode)
        HttpDownloader.resetRefuseCount(domain)


        handleHttpResponse(httpResp, taskEntity)


        if (!conf.getBoolean("local", false)) {
          requestCoutForbidden(taskEntity, domain, false)
          //if request count approach the requestCount in redis, then put the taskEntity into sortsetTask queue ,moreover you need set the interval for some threshold
          predictForbidden(domain, taskEntity)
        }
      }
    }
  }

  //change userAgent of domain
  def changeUserAgent(domain: String, taskEntity: CrawlerTaskEntity): Boolean = {
    var result = true
    AgentWorker.lock.synchronized {
      val array = AgentWorker.userAgentArray
      var tmpLog = 0
      if (!AgentWorker.userAgentMap.contains(domain)) {
        var in = 0
        in = array.indexOf(taskEntity.userAgent)

        if (taskEntity.userAgent != null && !taskEntity.userAgent.trim().equals("")) {
          if (taskEntity.userAgentType == 1) {
            //don't use mobile user agent
            breakable {
              while (true) {
                if (in <= -1 || in >= array.length - 1) in = -1
                if (array(in + 1)._2 == 2) {
                  in += 1
                } else break
              }
            }
          }
        }
        if (in <= -1 || in >= array.length - 1) in = -1
        AgentWorker.userAgentMap.put(domain, (in + 1, 1))
        taskEntity.userAgent = array(in + 1)._1
        logInfo("domain:" + domain + ",currentUserAgent:" + array(in + 1))
        //put the task into thread pool again
        AgentWorker.handleEntity(taskEntity, conf)
      } else {
        val tupleM = AgentWorker.userAgentMap.get(domain).get
        /* var index = tupleM._1 + 1
        if(index>array.length) index = 0*/
        var cnt = tupleM._2
        if (cnt >= array.size) {
          AgentWorker.userAgentMap.put(domain, (-1, 1))
          result = false
        } else {
          var index: Int = tupleM._1
          if (index <= -1 || index >= array.length - 1) index = -1
          if (taskEntity.userAgentType == 1) {
            //don't use mobile user agent
            breakable {
              while (true) {
                if (index <= -1 || index >= array.length - 1) index = -1
                if (array(index + 1)._2 == 2) {
                  index += 1
                } else break
              }
            }
          }
          if (index <= -1 || index >= array.length - 1) index = -1
          taskEntity.userAgent = array(index + 1)._1
          AgentWorker.userAgentMap.put(domain, (tupleM._1 + 1, tupleM._2 + 1))
          //put the task into thread pool again
          AgentWorker.handleEntity(taskEntity, conf)
          logInfo("domain:" + domain + ",currentUserAgent:" + array(index + 1))
        }
      }
    }
    result
  }

  //if request count approach the requestCount in redis, then put the taskEntity into sortsetTask queue ,moreover you need set the interval for some threshold
  private def predictForbidden(domain: String, taskEntity: CrawlerTaskEntity) = {
    if (HttpDownloader.countIntervel.contains(domain)) {
      HttpDownloader.countIntervel.get(domain) match {
        case Some(cacheTuple) =>
          var requestCount = HttpDownloader.forbiddenCountCacheManager.getIfPresent(domain)
          if (requestCount == null) {
            //get from redis
            val tmpFo = redis.forbiddenValue(domain)
            if (tmpFo != null) {
              HttpDownloader.forbiddenCountCacheManager.put(domain, tmpFo._2)
            }
          }
          var currentCnt = 0
          if (cacheTuple._3 != null) currentCnt = cacheTuple._3
          if (requestCount == null) requestCount = Integer.MAX_VALUE
          if ((cacheTuple._3 >= requestCount - conf.get("crawler.worker.sleep.cnt.forbidden", "10").toInt))
            redisTaskSortedSet.putElementToSortedSet(redisTaskSortedSetKey, taskEntity, System.currentTimeMillis() + 3 * 60 * 1000)
          logDebug("since forbidden predict ,put TaskEntity [" + taskEntity.taskId + "] to TaskSortedSet and retry it later")
        case None =>
          logError("no values in forbidded tuple!")
      }
    }
  }

  def requestCoutForbidden(taskEntity: CrawlerTaskEntity, domain: String, forbiddenStatus: Boolean) = {
    val tmpTime = System.currentTimeMillis()
    if (!HttpDownloader.countIntervel.contains(domain)) {
      if (!forbiddenStatus) HttpDownloader.countIntervel(domain) = (Long.MaxValue, tmpTime, 1, forbiddenStatus)
      else HttpDownloader.countIntervel(domain) = (tmpTime, Long.MaxValue, 1, forbiddenStatus)
    } else {
      HttpDownloader.countIntervel.get(domain) match {
        case Some(cacheTuple) =>
          var requestCount = -1
          var nomalInterval = -1L
          var forbiddenInterval = -1L
          if (cacheTuple._4 != forbiddenStatus) {
            //the status changed

            if (forbiddenStatus) {
              if (!changeUserAgent(domain, taskEntity)) redis.changeRefusedState(taskEntity.taskIp, domain, forbiddenStatus)
              HttpDownloader.lock.synchronized {
                val tmpTup = HttpDownloader.countIntervel.get(domain).get
                nomalInterval = tmpTime - tmpTup._2
                requestCount = tmpTup._3
                HttpDownloader.countIntervel(domain) = (tmpTime, tmpTup._2, 1, forbiddenStatus)
              }
              //put the nomalInterval and forbiddenStatus into redis
              //forbiddenStatus_requestCount_forbiddenInterval_normalInterval
              val tmpFo = redis.forbiddenValue(domain)
              if (tmpFo != null) {
                if (requestCount >= tmpFo._2) requestCount = tmpFo._2
                redis.changeForbiddenValue(domain, (forbiddenStatus, requestCount, tmpFo._3, nomalInterval))
                logInfo(s"domain[$domain],forbiddenStatus[$forbiddenStatus],nomalInterval[$nomalInterval],forbiddenInterval[$tmpFo._3]")
              }
            } else {
              redis.changeRefusedState(taskEntity.taskIp, domain, forbiddenStatus)
              HttpDownloader.lock.synchronized {
                val tmpTup = HttpDownloader.countIntervel.get(domain).get
                forbiddenInterval = tmpTime - tmpTup._1
                HttpDownloader.countIntervel(domain) = (tmpTup._1, tmpTime, 1, forbiddenStatus)
              }
              //put the nomalInterval and forbiddenStatus into redis
              val tmpFo = redis.forbiddenValue(domain)
              if (tmpFo != null)
                redis.changeForbiddenValue(domain, (forbiddenStatus, tmpFo._2, forbiddenInterval, tmpFo._4))
              logInfo(s"domain[$domain],forbiddenStatus[$forbiddenStatus],nomalInterval[$tmpFo._4],forbiddenInterval[$forbiddenInterval]")
            }
          } else {
            //status not changed,don't need sync to redis
            if (!forbiddenStatus) {
              val tmpTup = HttpDownloader.countIntervel.get(domain).get
              HttpDownloader.countIntervel(domain) = (tmpTup._1, tmpTup._2, tmpTup._3 + 1, tmpTup._4)
            } else {
              if (!changeUserAgent(domain, taskEntity)) redis.changeRefusedState(taskEntity.taskIp, domain, forbiddenStatus)
            }
          }
        case None => logError("not set values for forbidded tuple!")
      }
    }
  }

  //HTTP StatusCode 302
  private def handleHttpStatusCode302(taskEntity: CrawlerTaskEntity, httpResp: org.apache.http.HttpResponse): String = {
    var redirectResponseContent = ""
    val taskId = taskEntity.taskId
    val taskUrl = taskEntity.taskURI
    val location = httpResp.getFirstHeader("location").getValue
    logInfo(s"taskId[$taskId],taskUrl[$taskUrl] HTTP StatusCode 302, redirect location [$location]")
    var inputStream: InputStream = null
    var conn: URLConnection = null
    try {
      val redirection: URL = new URL(location)
      if (taskEntity.isUseProxy == 1 && !taskEntity.proxyHost.trim().equalsIgnoreCase("null")) {
        val proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(taskEntity.proxyHost, taskEntity.proxyPort));
        conn = redirection.openConnection(proxy)
      } else {
        conn = redirection.openConnection()
      }
      conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
      conn.setRequestProperty("Accept-Charset", "utf-8")
      conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8")
      conn.setRequestProperty("Connection", "keep-alive")
      conn.setRequestProperty("User-Agent", taskEntity.userAgent)
      conn.setRequestProperty("Cookie", taskEntity.cookies)
      inputStream = conn.getInputStream
      val bytes = new Array[Byte](inputStream.available())
      inputStream.read(bytes)
      redirectResponseContent = new java.lang.String(bytes)
    } catch {
      case t: Throwable => logError("HTTP 302 Redirection Error", t)
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close()
        } catch {
          case t: Throwable => logError("HTTP 302 Redirection Error", t)
        }
      }
    }
    redirectResponseContent
  }

  //HTTP request success, get response content and persist ResObj
  private def handleHttpResponse(httpResp: org.apache.http.HttpResponse, taskEntity: CrawlerTaskEntity): Unit = {
    val resp = new HttpResponse
    var content: String = ""
    var resultBytes: Array[Byte] = null
    val entity: HttpEntity = httpResp.getEntity()
    //println(EntityUtils.toString(entity))
    if (entity != null) {
      resultBytes = EntityUtils.toByteArray(entity);
      EntityUtils.consume(entity)
    }

    resp.code = httpResp.getStatusLine.getStatusCode
    resp.content = resultBytes
    val allHeaders = httpResp.getAllHeaders
    var respHeaders: Map[String, Seq[String]] = Map.empty
    for (header <- allHeaders) {
      respHeaders ++= (Map[String, Seq[String]](header.getName.toString -> Seq(header.getValue)))
    }
    resp.headers = respHeaders
    resp.uri = taskEntity.taskURI
    resp.contentType = respHeaders.get("Content-Type").get.mkString(";")
    val resObj = new ResObj(taskEntity, resp)
    val url = resp.uri
    val status = resp.code
    val taskId = taskEntity.taskId
    logInfo(s"taskId[$taskId], http requset Url[$url]completed, status[$status]")
    persist(resObj)
  }

  override def createRequest(): Request = {
    Request("httpClient", conf)
  }


  override def createTaskStreamProcess(): TaskStreamProcess = {
    TaskStreamProcess(conf, "kv")
  }


  override def createLocalTaskStreamProcess(): TaskStreamProcess = {
    TaskStreamProcess(conf, "local")
  }

  override def createStorageManager(): StorageManager = {
    StorageManager("oss", conf)
  }

  override def createMessageQueue: MessageQueue = {
    MessageQueue("kafka", conf)
  }

  // time logger

  override def logTimes(taskId: String, domain: String, ip: String, startTime: Long, endTime: Long, costTime: Long, statusCode: Int, refusedState: Boolean = false, content: String = ""): Boolean = {

    if (StringUtils.isBlank(content)) {
      logInfo(s"HttpRequestLog: taskId[$taskId], domain[$domain], ip[$ip], StartTime[$startTime], EndTime[$endTime], CostTime[$costTime], statusCode[$statusCode], refusedState[$refusedState]")
    } else {
      logInfo(s"HttpRequestLog: taskId[$taskId], domain[$domain], ip[$ip], StartTime[$startTime], EndTime[$endTime], CostTime[$costTime], statusCode[$statusCode], refusedState[$refusedState],\nresponseContent[$content]")
    }
    true
  }

  /*  private def storageManager(storage: String): StorageManager = {
    try {
      storage match {
        case "hbase"      => StorageManager("hbase", conf)
        case "qiniucloud" => StorageManager("qiniucloud", conf)
        case _            => StorageManager("hbase", conf)
      }
    } catch {
      case e: Exception => StorageManager("hbase", conf)
    }
  }*/

  def createTaskQueue(): ITaskQueue = {
    ITaskQueue("sortSet", conf)
  }

}

object HttpDownloader extends Logging {

  val cacheLoader: CacheLoader[java.lang.String, java.lang.Integer] =
    new CacheLoader[java.lang.String, java.lang.Integer]() {
      def load(key: java.lang.String): java.lang.Integer = {
        -1
      }
    }
  val forbiddenCountCacheManager = CacheBuilder.newBuilder()
    .expireAfterWrite(18, TimeUnit.HOURS).build(cacheLoader)

  //crawler forbidden 
  val lock = new AnyRef()
  //Map(domain->(forbiddenStartTime,forbiddenEndTime,requestCount,forbiddenStatus))
  //forbiddenStatus (true:forbidden,false:normal)
  val countIntervel = new HashMap[String, Tuple4[Long, Long, Int, Boolean]]()

  //HttpContext Attribute key 
  val TaskEntityId_Key = "taskEntity_Id"
  val TaskEntity_Starttime = "taskEntity_Starttime"

  //cache key[taskId]->Value[CrawlerTaskEntity]
  import scala.collection.JavaConversions._

  val TaskEntityHashMap = java.util.Collections.synchronizedMap(new scala.collection.mutable.HashMap[String, CrawlerTaskEntity])

  def getTaskEntityFromHttpClientContext(context: HttpClientContext): CrawlerTaskEntity = {
    // get taskId from HttpClientContext (HttpClientRequest.download put taskId into HttpClientContext)
    val taskId = context.getAttribute(HttpDownloader.TaskEntityId_Key).asInstanceOf[String]

    // get CrawlerTaskEntity from HttpDownloader.TaskEntityHashMap 
    // (HttpDownloader.asynfetch put CrawlerTaskEntity into HttpDownloader.TaskEntityHashMap)
    val taskEntity = HttpDownloader.TaskEntityHashMap.get(taskId).asInstanceOf[CrawlerTaskEntity]
    //val taskEntity = HttpDownloader.TaskEntityHashMap.get(taskId).getOrElse(null).asInstanceOf[CrawlerTaskEntity]

    if (taskEntity == null) {
      logError(s"HttpClientRequest.TaskEntityHashMap [$taskId] TaskEntity Not found")
    } else {
      // delete CrawlerTaskEntity cache
      HttpDownloader.TaskEntityHashMap.remove(taskId)
    }
    taskEntity
  }

  //cache key[ip-domain]->value[refusedState]
  private val refusedCountHashMap = new scala.collection.parallel.mutable.ParHashMap[String, Int]

  private var refusedCountMax = 3

  private def updateRefuseCountHashMap(domain: String): Int = {
    refusedCountHashMap.synchronized {
      var refusedCount = refusedCountHashMap.get(domain).getOrElse(0)
      refusedCount += 1
      refusedCountHashMap.update(domain, refusedCount)
      logDebug(s"domain[$domain] latest refusedCount[$refusedCount]")
      refusedCount
    }
  }

  private def resetRefuseCount(domain: String, value: Int = 0) {
    refusedCountHashMap.synchronized {
      refusedCountHashMap.update(domain, value)
    }
  }

  var httpDownloader: HttpDownloader = null

  def apply(conf: CrawlerConf): HttpDownloader = {
    if (httpDownloader == null) httpDownloader = new HttpDownloader(conf)
    httpDownloader
  }

}

object TestHttpDownloader {
  def main(args: Array[String]): Unit = {
    testChangeUserAgent
    //println(("ss", 1).toString())
    //testRequestCoutForbidden
    //testRequestCountForbidden2

  }

  def testRequestCountForbidden2 = {
    val conf = new CrawlerConf
    val hanlder = HttpDownloader(conf)
    val d = new CrawlerTaskEntity
    d.userAgent = "User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; ) Opera/UCWEB7.0.2.37/28/999"
    hanlder.requestCoutForbidden(d, "ha", false)
    hanlder.requestCoutForbidden(d, "ha", false)
    hanlder.requestCoutForbidden(d, "ha", true)
    hanlder.requestCoutForbidden(d, "ha", true)
    hanlder.requestCoutForbidden(d, "ha", true)
    hanlder.requestCoutForbidden(d, "ha", true)
  }

  def testRequestCoutForbidden() = {
    HttpDownloader.countIntervel("ha") = (Long.MaxValue, System.currentTimeMillis(), 1, false)
    AgentWorker.userAgentMap("ha") = (34, 35)
    //AgentWorker.userAgentMap("ha") = (35, 36)
    val conf = new CrawlerConf
    val hanlder = HttpDownloader(conf)
    val d = new CrawlerTaskEntity
    d.userAgent = "User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; ) Opera/UCWEB7.0.2.37/28/999"
    hanlder.requestCoutForbidden(d, "ha", true)
  }

  def testChangeUserAgent = {
    val conf = new CrawlerConf
    val hanlder = HttpDownloader(conf)
    val d = new CrawlerTaskEntity
    var c = 0
    for (i <- 1 to 2 * AgentWorker.userAgentArray.length + 3) {
      if (hanlder.changeUserAgent("finance.eastmoney.com", d)) {
        c += 1
        println(d.userAgent + "index:" + i)
      }
    }
    println("size: " + (2 * AgentWorker.userAgentArray.length + 3) + "true:" + c + "useral:" + AgentWorker.userAgentArray.length)
  }

  def test = {
    val conf = new CrawlerConf
    val hanlder = HttpDownloader(conf)
    val urls = Seq("http://finance.eastmoney.com/news/1344,20150716527836591.html", "http://daily.zhihu.com/story/4857231?utm_campaign=in_app_share&utm_medium=iOS&utm_source=sina", "https://www.baidu.com/index.php?tn=56060048_4_pg&ch=12")
    for (i <- 0 to 20) {
      hanlder.asynfetch(testTaskEntity(urls(1)))
    }

  }

  def test2 = {
    val conf = new CrawlerConf
    val hanlder = HttpDownloader(conf)
    val tp = com.foofv.crawler.util.Util.newDaemonFixedThreadPool(100, "test")
    val urls = Seq("http://finance.eastmoney.com/news/1344,20150716527836591.html", "http://daily.zhihu.com/story/4857231?utm_campaign=in_app_share&utm_medium=iOS&utm_source=sina", "https://www.baidu.com/index.php?tn=56060048_4_pg&ch=12")
    for (i <- 0 to 10) {
      tp.execute(new TestRunnable(hanlder, urls(1)))
    }
    tp.shutdown()
    tp.awaitTermination(10, java.util.concurrent.TimeUnit.MINUTES)
    if (tp.isTerminated()) {
      println("done")
    }
  }

  def testTaskEntity(uri: String = "http://waimai.meituan.com/restaurant/177274"): CrawlerTaskEntity = {
    val task = new CrawlerTaskEntity
    task.jobId = 1
    task.jobName = "job.jobName"
    task.taskType = CrawlerTaskType.THEME_CRAWLER
    task.taskURI = uri
    task.taskDomain = "waimai.meituan.com"
    task.cookies = "uuid=54143f77c4a0b25d094f.1437098963.0.0.0; oc=EVvRaqT96vDLZrQzSZE_bNKjvuLSZF57uAqLRcPToaXmBC6krJ8BNT2mkjzyg40X2vPfrSa0zsy4RCvWbhaJUWi7DXDiDXynyvUE3wVyyf3atPRdUdAa5AnNEwy1GGuv_oAX-_93dd0jCoRcz6oTE-1vcmhvb1kVaJ6uOSgx12g; ci=10; abt=1437098963.0%7CADE; REMOVE_ACT_ALERT=1; w_uuid=676619b7-cb75-4be9-9aca-f90d631c6e0e; w_cid=110100; w_cpy_cn=\"%E5%8C%97%E4%BA%AC\"; w_cpy=beijing; waddrname=\"%E5%A4%A9%E5%AE%89%E9%97%A8%2C%E5%89%8D%E9%97%A8%2C%E5%92%8C%E5%B9%B3%E9%97%A8\"; w_geoid=wx4g0f4keyrj; w_ah=\"39.91413692012429,116.4034067094326,%E5%A4%A9%E5%AE%89%E9%97%A8%2C%E5%89%8D%E9%97%A8%2C%E5%92%8C%E5%B9%B3%E9%97%A8\"; w_utmz=\"utm_campaign=(direct)&utm_source=(direct)&utm_medium=(none)&utm_content=(none)&utm_term=(none))))\"; w_visitid=82135df1-e0f4-4ab7-9ef2-5ad2e0e8f9cc; JSESSIONID=oi8ovxzyelte2dxhleuzckas; __mta=216488873.1437099000064.1437099042163.1437099061817.4"
    task.isUseProxy = 0
    task.totalDepth = 1
    task.intervalTime = 0
    task.httpRefer = "http://waimai.meituan.com/home/wx4g0f4keyrj"
    task
  }
}

class TestRunnable(handler: HttpDownloader, url: String) extends Runnable {

  def run() {
    handler.asynfetch(TestHttpDownloader.testTaskEntity())
  }
}
