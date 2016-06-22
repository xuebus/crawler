package com.foofv.crawler.agent

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.CrawlerException
import com.foofv.crawler.deploy.TransferMsg.RegisterWorker
import com.foofv.crawler.parse.topic.SchemeTopicParser
import com.foofv.crawler.parse.worker.WorkerArguments
import com.foofv.crawler.resource.slave.Worker
import com.foofv.crawler.util.AkkaUtil
import akka.actor.ActorSelection.toScala
import akka.actor.{ ActorRef, ActorSystem, Address, Props }
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.util.Util
import com.foofv.crawler.downloader.TaskRunner
import com.foofv.crawler.downloader.Downloader
import java.util.concurrent.ThreadPoolExecutor
import com.foofv.crawler.util.Logging
import com.foofv.crawler.util.constant.Constant
import scala.collection.mutable.HashMap
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.enumeration.HttpRequestMethodType
import com.foofv.crawler.enumeration.CrawlerTaskType
import com.google.common.cache.CacheBuilder
import com.foofv.crawler.antispamming.AntiSpamming
import com.google.common.cache.CacheLoader
import com.foofv.crawler.antispamming.IAntiSpammingFactory
import java.util.concurrent.TimeUnit
import com.foofv.crawler.antispamming.RedisAntiSpamming
import scala.collection.mutable.ArrayBuffer
import com.foofv.crawler.enumeration.CrawlerDTWorkerType

/**
 * do work
 */
private[crawler] class AgentWorker(
  override val host: String,
  override val port: Int,
  override val cores: Int,
  override val memory: Int,
  override val masterUrls: Array[String],
  override val actorSystemName: String,
  override val actorName: String,
  override val conf: CrawlerConf) extends Worker(host, port, cores, memory, masterUrls, actorSystemName, actorName, conf) with IAntiSpammingFactory {

  var cralwerUserAgentTask: CrawlerDTWorker = null
  val USERAGENT = "useragent"

  val pullUserAgentintervalTimeSec = Constant(conf).CRAWLER_AGENT_WORKER_PULL_USERAGENT_INTERVAL
  cralwerUserAgentTask = new CrawlerDTWorker(name = "AgentWorkerPullUserAgentTask", interval = pullUserAgentintervalTimeSec, callback = pullAllUserAgent)
  cralwerUserAgentTask.startUp()

  val pullHttpproxyintervalTimeSec = Constant(conf).CRAWLER_AGENT_WORKER_PULL_HTTPPROXY_INTERVAL
  val cralwerPullHttpProxyTask = new CrawlerDTWorker(name = "AgentWorkerPullHttpProxyTask", interval = pullHttpproxyintervalTimeSec, callback = pullProxyFromRedis)
  cralwerPullHttpProxyTask.startUp()

  val redisTaskSortedSetKey = Constant(conf).CRAWLER_TASK_SORTEDSET_KEY
  val redisTaskSortedSet: ITaskQueue = ITaskQueue("sortSet", conf)
  val redisSet = ITaskQueue("set", conf)

  logInfo("AgentWorker start up")

  //test
  //testMeituanFoodMerchantTask

  def test = {
    val task = new CrawlerTaskEntity
    task.taskId = "123"
    task.taskURI = "http://waimai.meituan.com/restaurant/50?pos=20"
    //task.taskURI = "http://waimai.meituan.com/ajax/comment"
    task.cookies = "REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96'; w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; " +
      "JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\",";
    task.httpRefer = "http://waimai.meituan.com/home/wx4g1ypnct88"
    //task.httpRefer = "http://waimai.meituan.com/restaurant/212018?pos=20"
    //task.httpmethod=HttpRequestMethodType.POST
    //task.parentTaskToParames="{\"paramsData\":{\"wmpoiIdStr\":\"187701\",\"offset\":\"1\",\"has_content\":\"0\",\"score_grade\":\"0\"}}"
    task.keyWordsOfInvalid = "offline"
    for (i <- 1 to 3) {
      Thread.sleep(3000)
      handleTaskEntity(task)
    }
  }

  def testMeituanFoodMerchantTask = {
    val task = new CrawlerTaskEntity
    task.forbiddenCode = "429"
    task.jobId = 99
    task.jobName = "testMeituanFoodMerchantTask"
    task.totalDepth = 5
    task.taskDomain = "www.meituan.com"
    task.taskType = CrawlerTaskType.THEME_CRAWLER
    task.taskURI = "http://www.meituan.com/index/changecity/initiative?mtt=1.index%2Ffloornew.0.0.idgs0y4o"
    task.cookies = "REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96'; w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; " +
      "JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\",";
    task.httpRefer = "http://bj.meituan.com/?mtt=1.index%2Fchangecity.0.0.idgs0wc5"
    task.topciCrawlerParserClassName = "com.foofv.crawler.parse.topic.MeituanFoodMerchantParser"
    Thread.sleep(3000)
    handleTaskEntity(task)
  }

  override def changeMaster(url: String) = {
    activeMasterUrl = url
    master = context.actorSelection(AgentMaster.toAkkaUrl(activeMasterUrl))
    masterAddress = activeMasterUrl match {
      case AgentMaster.crawlerUrlRegex(_host, _port) =>
        Address("akka.tcp", AgentMaster.systemName, _host, _port.toInt)
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
      val actor = context.actorSelection(AgentMaster.toAkkaUrl(masterUrl))
      actor ! RegisterWorker(workerId, host, port, cores, memory)
    }
  }

  private def pullAllUserAgent(): Long = {
    try {
      val a = redisSet.getAllFromSetByKey(USERAGENT)
      if (a != null && !a.isEmpty && a.size > 0) AgentWorker.userAgentArray = a.map(s => (s.utf8String.split(Constant(conf).USERAGENT_SEPATOR)(0), s.utf8String.split(Constant(conf).USERAGENT_SEPATOR)(1).toInt)).toArray
    } catch {
      case e: Exception => logError("useragent null!", e)
    }
    22 * 60 * 60 * 1000
  }

  override def handleTaskEntity(taskEntity: CrawlerTaskEntity) {
    try {
      var task: CrawlerTaskEntity = taskEntity
      if (taskEntity.isUseProxy == 1) {
        task = setHttpProxy(taskEntity)
      }
      if (task != null) {
        AgentWorker.handleEntity(taskEntity, conf)
      }
    } catch {
      case t: Throwable => logError("get http_proxy error", t)
    }
  }

  private val HttpProxySeparator = Constant(conf).SEPATOR
  private val invalidHttpProxySet = Constant.CRAWLER_AGENT_WORKER_INVALID_HTTP_PROXY
  private def pullProxyFromRedis(): Long = {
    try {
      val HttpProxyKey = Worker._workerId + "_http_proxy"
      if (AgentWorker.invalidProxyArray.size > 0) {
        val invalidProxys = AgentWorker.invalidProxyArray
        logInfo(s"remove invalid proxy[$invalidProxys]")
        redisSet.removeElementFromSetByKey(HttpProxyKey, invalidProxys.toSeq)
        AgentWorker.invalidProxyArray.foreach { x => redisSet.putToSet(invalidHttpProxySet, HttpProxyKey + "_" + x) }
        AgentWorker.invalidProxyArray.clear()
      }
      
      logInfo(s"HttpProxyKey[$HttpProxyKey]")
      val a = redisSet.getAllFromSetByKey(HttpProxyKey)
      AgentWorker.httpProxyArray.clear()
      if (a != null && !a.isEmpty && a.size > 0) {
        val temp = a.map(s => (s.utf8String.split(HttpProxySeparator)(0), s.utf8String.split(HttpProxySeparator)(1).toInt)).toBuffer
        AgentWorker.httpProxyArray.++=(temp)
      }
      logInfo("avaliable proxy:[" + AgentWorker.httpProxyArray + "]")
    } catch {
      case e: Exception => logError("get http_proxy error", e)
    }
    
    //put failed task back to SortedSet
    try {
      val tempFailedTaskEntityList = AgentWorker.failedTaskEntitySet.toList
      AgentWorker.failedTaskEntitySet.clear()
      tempFailedTaskEntityList.foreach { x =>
        {
          if (x != null) {
            logInfo("put failed task[" + x.taskId + "] back to SortedSet")
            redisTaskSortedSet.putElementToSortedSet(redisTaskSortedSetKey, x, System.currentTimeMillis() + 5 * 60 * 1000)
          }
        }
      }
    } catch {
      case t: Throwable => logError("put failed task back to SortedSet error", t)
    }
    -1
  }

  def createAntiSpamming: AntiSpamming = {
    new RedisAntiSpamming(conf)
  }

  val antiSpam: AntiSpamming = createAntiSpamming

  val cacheLoader: CacheLoader[java.lang.String, java.lang.Boolean] =
    new CacheLoader[java.lang.String, java.lang.Boolean]() {
      def load(key: java.lang.String): java.lang.Boolean = {
        false
      }
    }
  val antiSpammingCacheManager = CacheBuilder.newBuilder()
    .expireAfterWrite(60 * 1000, TimeUnit.MILLISECONDS).build(cacheLoader)

  private def checkAntiSpamming(ip: String, domain: String): Boolean = {
    val key = ip.trim() + "_" + domain.trim() + "_isRefused"
    var value = antiSpammingCacheManager.getIfPresent(key)
    if (value == null) {
      value = antiSpam.isRefused(ip.trim(), domain.trim())
      antiSpammingCacheManager.put(key, value)
    }
    logInfo(s"checkAntiSpamming result: Key[$key], Value[$value]")
    value
  }

  // (domain, ip, timestamp)
  private val handleTaskRecords = new ArrayBuffer[(String, String, Long)]()
  private val handleTaskCountPerMinute = Constant(conf).CRAWLER_AGENT_WORKER_PROXY_HANDLE_TASK_COUNT_PER_MINUTE_PROXYHOST
  private val handleTaskTooFastDelayTime = Constant(conf).CRAWLER_AGENT_WOEKER_HANDLE_TASK_DELAY_TIME_IF_TOO_FAST

  private def setHttpProxy(taskEntity: CrawlerTaskEntity): CrawlerTaskEntity = {
    var task: CrawlerTaskEntity = null
    val taskDomain = taskEntity.taskDomain
    val taskId = taskEntity.taskId
    val now = System.currentTimeMillis()
    // refresh handleTaskCache
    val outOfDateRecords = handleTaskRecords.filter(p => now - p._3 > 1 * 60 * 1000)
    logInfo(s"outOfDateRecords[$outOfDateRecords]")
    handleTaskRecords --= outOfDateRecords
    // if no http proxy
    if (AgentWorker.httpProxyArray.length == 0) {
      task = useWorkerIpIfNoProxy(taskEntity)
    } else {
      // find avaliable proxy from AgentWorker.httpProxyArray
      var proxyUsedTimes = 0
      val proxyOption = AgentWorker.httpProxyArray.find(x => {
        proxyUsedTimes = handleTaskRecords.filter(p => p._1.equalsIgnoreCase(taskEntity.taskDomain) && p._2.equalsIgnoreCase(x._1)).size
        proxyUsedTimes < handleTaskCountPerMinute && !checkAntiSpamming(x._1, taskDomain)
      })
      if (proxyOption.nonEmpty) {
        val (proxyHost, proxyPort) = proxyOption.get
        taskEntity.proxyHost = proxyHost
        taskEntity.proxyPort = proxyPort
        logInfo(s"set TaskEntity[$taskId] domain[$taskDomain]  proxy[$proxyHost:$proxyPort], proxyUsedTimes[$proxyUsedTimes]")
        AgentWorker.httpProxyArray -= ((proxyHost, proxyPort))
        AgentWorker.httpProxyArray = AgentWorker.httpProxyArray :+ ((proxyHost, proxyPort))
        handleTaskRecords.+=:((taskDomain, proxyHost, now))
        task = taskEntity
      } else {
        task = useWorkerIpIfNoProxy(taskEntity)
      }
    }
    task
  }

  val workerHandleTaskCountPerMinute = Constant(conf).CRAWLER_AGENT_WORKER_HANDLE_TASK_COUNT_PER_MINUTE_PROXYHOST
  val workerSleepTimeIfRefused = Constant(conf).CRAWLER_AGENT_WORKER_SLEEP_TIME_IF_REFUSED
  private def useWorkerIpIfNoProxy(taskEntity: CrawlerTaskEntity): CrawlerTaskEntity = {
    val delayTime = handleTaskTooFastDelayTime * 5
    val workerIp = Worker.WorkerIp
    var task: CrawlerTaskEntity = null
    val taskDomain = taskEntity.taskDomain
    val taskId = taskEntity.taskId
    val now = System.currentTimeMillis()
    logInfo(s"no avaliable http proxy for TaskEntity[$taskId]  domain[$taskDomain]")
    val workerIpUsedTimes = handleTaskRecords.filter(p => { p._1.equalsIgnoreCase(taskEntity.taskDomain) && p._2.equalsIgnoreCase(workerIp) }).size
    if (workerIpUsedTimes >= workerHandleTaskCountPerMinute) {
      logInfo(s"workerIpUsedTimes[$workerIpUsedTimes] too fast, AgentWorker[$workerIp] is not avaliable, put TaskEntity[$taskId]  domain[$taskDomain] to TaskSortedSet, retry [$delayTime] millionseconds later.")
      redisTaskSortedSet.putElementToSortedSet(redisTaskSortedSetKey, taskEntity, System.currentTimeMillis() + delayTime)
    } else if (checkAntiSpamming(workerIp, taskDomain)) {
      logInfo(s"AgentWorker[$workerIp] is refused, put TaskEntity[$taskId]  domain[$taskDomain] to TaskSortedSet, retry other AgentWorkers [$delayTime] millionseconds later.")
      redisTaskSortedSet.putElementToSortedSet(redisTaskSortedSetKey, taskEntity, System.currentTimeMillis() + delayTime)
      antiSpam.changeRefusedState(Worker._workerId, taskDomain, true, workerSleepTimeIfRefused)
    } else {
      // use Worker.WorkerIp
      logInfo(s"workerIpUsedTimes[$workerIpUsedTimes], use Workerip[$workerIp]  for TaskEntity[$taskId]  domain[$taskDomain]")
      taskEntity.proxyHost = "null"
      handleTaskRecords.+=:((taskDomain, workerIp, now))
      task = taskEntity
    }
    task
  }

  override def activeThreadCount: Int = {
    if (AgentWorker.agentWorkerThreadPool != null) AgentWorker.agentWorkerThreadPool.getActiveCount
    else 0
  }

}

private[crawler] object AgentWorker extends Logging {

  private val failedTaskEntitySet = new scala.collection.mutable.HashSet[CrawlerTaskEntity]()

  def putFailedTaskBacktoSortedSet(taskEntity: CrawlerTaskEntity) {
    if (taskEntity != null) {
      val taskId = taskEntity.taskId
      logInfo(s"task[$taskId] failed, put it back to SortedSet later")
      failedTaskEntitySet.add(taskEntity)
    }
  }

  // (ip, timestamp)
  private val timeOutProxyBuffer = new ArrayBuffer[(String, Long)]()
  private val invalidProxyArray = new scala.collection.mutable.HashSet[String]()

  def addTimeoutProxyRecord(proxyHost: String) {
    if (proxyHost != null) {
      if (!proxyHost.contains(Worker.WorkerIp)) {
        logInfo(s"proxyHost[$proxyHost] may be not avaliable")
        val now = System.currentTimeMillis()
        timeOutProxyBuffer.+=:(proxyHost, System.currentTimeMillis())
        val outOfDateCache = timeOutProxyBuffer.filter(p => now - p._2 > 3 * 60 * 1000)
        timeOutProxyBuffer --= outOfDateCache
        val recentTimeoutCount = timeOutProxyBuffer.filter(_._1.contains(proxyHost)).size
        if (recentTimeoutCount > 20) {
          logInfo(s"proxy[$proxyHost] is not avaliable, remove it later")
          invalidProxyArray.+=(proxyHost)
          val proxyOption = httpProxyArray.find(p => proxyHost.contains(p._1))
          if (proxyOption.nonEmpty) {
            httpProxyArray -= (proxyOption.get)
          }
        }
      }
    }
  }

  //(proxy, port)
  var httpProxyArray: ArrayBuffer[(String, Int)] = new ArrayBuffer[(String, Int)]

  var userAgentArray: Array[(String, Int)] = Array(
    ("User-Agent:Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50", 1), //safari 5.1 – MAC
    ("User-Agent:Mozilla/5.0 (Windows; U; Windows NT 6.1; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50", 1), //safari 5.1 – Windows
    ("User-Agent:Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0", 1), //IE 9.0
    ("User-Agent:Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0)", 1), //IE 8.0
    ("User-Agent:Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)", 1), //IE 7.0
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)", 1), //IE 6.0
    ("User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0", 1), //Firefox 38.0.1
    ("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:2.0.1) Gecko/20100101 Firefox/4.0.1", 1), //Firefox 4.0.1 – MAC
    ("User-Agent:Mozilla/5.0 (Windows NT 6.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1", 1), //Firefox 4.0.1 – Windows

    ("User-Agent:Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; en) Presto/2.8.131 Version/11.11", 1), //Opera 11.11 – MAC
    ("User-Agent:Opera/9.80 (Windows NT 6.1; U; en) Presto/2.8.131 Version/11.11", 1), //Opera 11.11 – Windows
    ("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_0) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11", 1), //Chrome 17.0 – MAC
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Maxthon 2.0)", 1), //（Maxthon）
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; TencentTraveler 4.0)", 1), //QQ TT
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)", 1), //The World 2.x
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; The World)", 1), //The World 3.x
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; SE 2.X MetaSr 1.0; SE 2.X MetaSr 1.0; .NET CLR 2.0.50727; SE 2.X MetaSr 1.0)", 1), //sougou 1.x
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; 360SE)", 1), //360
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Avant Browser)", 1), //Avant
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)", 1), //Green Browser
    ("User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 LBBROWSER", 1), // Liebao Brower

    //mobile
    ("User-Agent:Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_3 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5", 2), //safari iOS 4.33 – iPhone
    ("User-Agent:Mozilla/5.0 (iPod; U; CPU iPhone OS 4_3_3 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5", 2), //safari iOS 4.33 – iPod Touch
    ("User-Agent:Mozilla/5.0 (iPad; U; CPU OS 4_3_3 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5", 2), //safari iOS 4.33 – iPad
    ("User-Agent: Mozilla/5.0 (Linux; U; Android 2.3.7; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1", 2), //Android N1
    ("User-Agent: MQQBrowser/26 Mozilla/5.0 (Linux; U; Android 2.3.7; zh-cn; MB200 Build/GRJ22; CyanogenMod-7) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1", 2), //Android QQ浏览器 For android
    ("User-Agent: Opera/9.80 (Android 2.3.4; Linux; Opera Mobi/build-1107180945; U; en-GB) Presto/2.8.149 Version/11.10", 2), //Android Opera Mobile
    ("User-Agent: Mozilla/5.0 (Linux; U; Android 3.0; en-us; Xoom Build/HRI39) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13", 2), //Android Pad Moto Xoom
    ("User-Agent: Mozilla/5.0 (BlackBerry; U; BlackBerry 9800; en) AppleWebKit/534.1+ (KHTML, like Gecko) Version/6.0.0.337 Mobile Safari/534.1+", 2), //BlackBerry
    ("User-Agent: Mozilla/5.0 (hp-tablet; Linux; hpwOS/3.0.0; U; en-US) AppleWebKit/534.6 (KHTML, like Gecko) wOSBrowser/233.70 Safari/534.6 TouchPad/1.0", 2), //WebOS HP Touchpad
    ("User-Agent: Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 NokiaN97-1/20.0.019; Profile/MIDP-2.1 Configuration/CLDC-1.1) AppleWebKit/525 (KHTML, like Gecko) BrowserNG/7.1.18124", 2), //Nokia N97
    ("User-Agent: Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; Titan)", 2), //Windows Phone Mango
    ("User-Agent: UCWEB7.0.2.37/28/999", 2), //UC
    ("User-Agent: NOKIA5700/ UCWEB7.0.2.37/28/999", 2), //UC stand
    ("User-Agent: Openwave/ UCWEB7.0.2.37/28/999", 2), //UCOpenwave
    ("User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; ) Opera/UCWEB7.0.2.37/28/999", 2) //UC Opera
    )
  //domain->(currentUserAgentIndex,cout)
  val userAgentMap = new HashMap[String, (Int, Int)]()
  val lock = new AnyRef()

  private var agentWorkerThreadPool: ThreadPoolExecutor = null

  var workerActor: ActorRef = null

  def main(argStrings: Array[String]): Unit = {

    val conf = new CrawlerConf
    val args = new WorkerArguments(argStrings, conf)
    Worker.WorkerIp = args.host
    //args.masters(0) = "crawler://192.168.1.202:10000"
    //args.masters(0) = "crawler://192.168.10.25:9999"
    //args.masters(0) = "crawler://192.168.10.47:9999"
    //args.masters(0) = "crawler://192.168.10.146:9999"

    val (actorSystem, _) = startSystemAndActor(args.host, args.port, args.cores, args.memory, args.masters)
    actorSystem.awaitTermination()

  }

  def handleEntity(taskEntity: CrawlerTaskEntity, conf: CrawlerConf) = {
    if (agentWorkerThreadPool == null) agentWorkerThreadPool = Util.newDaemonFixedThreadPool(conf.getInt("crawler.agent.worker.threadnum", Util.inferCores()), "agent thread excutor")
    val taskId = taskEntity.taskId
    val taskUrl = taskEntity.taskURI
    if (taskEntity.isUseProxy == 1 && !taskEntity.proxyHost.trim().equalsIgnoreCase("null")) {
      taskEntity.taskIp = taskEntity.proxyHost
    } else {
      taskEntity.taskIp = Worker.WorkerIp
    }
    logInfo(s"begin handle task [$taskId] [$taskUrl]")
    val downloader = Downloader("http", conf)
    agentWorkerThreadPool.execute(new TaskRunner(taskEntity, downloader))
  }

  def startSystemAndActor(
    host: String,
    port: Int,
    cores: Int,
    memory: Int,
    masterUrls: Array[String],
    workerNumber: Option[Int] = None): (ActorSystem, Int) = {

    val conf = new CrawlerConf
    val systemName = "crawlerWorker" + workerNumber.map(_.toString).getOrElse("")
    val actorName = "AgentWorker"
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    workerActor = actorSystem.actorOf(Props(classOf[AgentWorker], host, boundPort, cores, memory,
      masterUrls, systemName, actorName, conf), name = actorName)
    //new SchemeTopicParser(new CrawlerConf()).testWorkerActor

    (actorSystem, boundPort)
  }

}
