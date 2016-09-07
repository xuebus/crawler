package com.foofv.crawler.control

import java.net.URLEncoder

import com.foofv.crawler.parse.scheme.AnalyseSemanticTree
import com.foofv.crawler.parse.scheme.nodes.{DocumentNode, SemanticNode}
import com.foofv.crawler.sql.SQLParser
import com.foofv.crawler.util.{Util, ActorLogReceive, Logging}
import com.foofv.crawler.deploy.TransferMsg._
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.{CrawlerTaskEntity, Job}
import com.foofv.crawler.enumeration._
import com.foofv.crawler.parse.topic.entity.{AddressInfo, TakeoutMerchant}
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.storage.MongoStorage
import com.foofv.crawler.util.listener.{ManagerListenerWaiter, JobStarted, TraceListenerWaiter}
import org.apache.commons.lang3.StringUtils
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map, HashMap}
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._
import com.foofv.crawler.util.constant.Constant
import scala.collection.JavaConverters
import java.util.Collections
import akka.actor.ActorRef
import scala.collection.mutable.Map

private[crawler] class CrawlerControlImpl private(conf: CrawlerConf) extends CrawlerControl with Logging {

  /*private val taskQueue: ITaskQueue = ITaskQueue("sortSet", conf)
  private val redis: ITaskQueue = ITaskQueue("list", conf)*/
  private val taskQueue: ITaskQueue = conf.taskQueue
  private val redis: ITaskQueue = conf.redis
  private val userAgentSetName: String = conf.get("crawler.control.userAgent.set.name", "user-agent-set")
  private val sqlHelp = SQLParser("mongo", conf)
  val w = ManagerListenerWaiter()


  //init
  def init() {}

  def addSeedTaskByJson(seeds: String): String = {
    var result = ""
    var faieldMsg = "system error"
    try {
      val job = new Job
      val taskList = getTaskEntityFromJson(seeds, job)
      if (taskList != null && taskList.size > 0) {
        val schemeTaskList = taskList.filter(s => s.schemeFile != null && !s.schemeFile.trim.equalsIgnoreCase("null"))
        val schemeProcessTaskList = schemeTaskList.flatMap(parseSchemeFile(_))
        var lastSubmitTaskList = taskList.filterNot(s => s.schemeFile != null && !s.schemeFile.trim.equalsIgnoreCase("null")) ++ schemeProcessTaskList.filter(_ != null)
        // lastSubmitTaskList.foreach { task => submitTask(task) }
        /* var sqlSubmitTaskList: Seq[CrawlerTaskEntity] = null
         if(sql !=null) sqlSubmitTaskList = geneTaskBySql(sqlHelp.parse(sql))
          if(sqlSubmitTaskList != null) lastSubmitTaskList = lastSubmitTaskList ++ sqlSubmitTaskList*/
        var lastSubmitTaskFilter = lastSubmitTaskList.filter { task => submitTask(task) }
        w.post(JobStarted(job.jobId.toString, job.jobName.toString, lastSubmitTaskFilter.size))
        lastSubmitTaskFilter = null
        result = "\"jobId:\"" + job.jobId + ",\"jobName\":\"" + job.jobName + "\""
      } else {
        faieldMsg = "no task can run."
        result = "\"failed:\"" + faieldMsg + "\""
      }
    } catch {
      case t: Throwable =>
        logError("addSeedTaskByJson error", t)
        result = "\"failed:\"" + faieldMsg + "\""
    }
    result
  }

  def parseSchemeFile(taskEntity: CrawlerTaskEntity): Seq[CrawlerTaskEntity] = {

    val t = new AnalyseSemanticTree().newDocEl(taskEntity.schemeFile).tree
    if (t != null) {
      taskEntity.totalDepth = t.size
      var sql: String = null
      var cnt = 0
      breakable {
        t.foreach { n =>
          val d = n._2.asInstanceOf[DocumentNode]
          if (d.firstDoc) {
            if (d.tableName != null) taskEntity.tableName = d.tableName
            if (d.isStream) taskEntity.isStream = 1
            sql = d.sql
            setTaskUrl(taskEntity, n)
            break
          } else cnt += 1
        }
      }
      if (cnt == t.size) {
        val m = t.take(1)
        val v = m.toVector
        val tup = v.get(0)
        setTaskUrl(taskEntity, tup)
      }

      var sqlSubmitTaskList: Seq[CrawlerTaskEntity] = null
      try {
        if (sql != null) sqlSubmitTaskList = geneTaskBySql(taskEntity, sqlHelp.parse(sql))
      }
      catch {
        case e: Exception => logError("sql parser faield!", e)
      }

      CrawlerControlImpl.semanticTreeMap(taskEntity.jobId.toString) = t

      if (sqlSubmitTaskList != null && sqlSubmitTaskList.size > 0) sqlSubmitTaskList else List(taskEntity)
    } else null
  }

  def geneTaskBySql(taskEntity: CrawlerTaskEntity, sqlResult: Seq[Map[String, AnyRef]]): Seq[CrawlerTaskEntity] = {
    val sTasks = for (r <- sqlResult) yield genTaskEntityBySql(taskEntity, r)
    if (sTasks != null && sTasks.size > 0) sTasks else null
  }

  def genTaskEntityBySql(taskEntity: CrawlerTaskEntity, r: Map[String, AnyRef]): CrawlerTaskEntity = {
    val cloneChildTaskEntity = taskEntity.allCloneSelf()
    var url: String = cloneChildTaskEntity.taskURI
    if (url != null && !url.trim.isEmpty) {
      var regexModel = "(\\$\\{model\\})"
      var regexModelTmp = "(\\$\\{model\\})"
      r.foreach { sM =>
        val key = sM._1
        val valu = sM._2.toString
        val matchFirstList = Util.regexExtract(url, Constant(conf).DOLLAR).asInstanceOf[Seq[String]]
        val regexMd = regexModelTmp.replaceAll("model", key)
        breakable {
          for (m <- matchFirstList) {
            if (key.trim.equalsIgnoreCase(m)) {
              url = url.replaceAll(regexMd, valu)
              break
            }
          }
        }
      }
      cloneChildTaskEntity.taskURI = url
    }
    cloneChildTaskEntity
  }

  def setTaskUrl(taskEntity: CrawlerTaskEntity, n: (String, SemanticNode)): Unit = {
    taskEntity.schemeDocId = n._1
    val taskUrl = taskEntity.taskURI
    if (taskUrl == null || taskUrl.trim.isEmpty || taskUrl.trim.equalsIgnoreCase("null")) {
      var url = n._2.asInstanceOf[DocumentNode].docUrl
      //val model: String = "http://model"
      //url = url.replaceFirst("http://", "")
      // url = URLEncoder.encode(url, "utf-8")
      //url = model.replaceFirst("model", url)
      url = url.replaceAll("\\|", "%7c")
      taskEntity.taskURI = url
    }
    taskEntity.taskDomain = getDomain(n._2.asInstanceOf[DocumentNode].docUrl, taskEntity.taskDomain)
  }

  def getTaskEntityFromJson(content: String, job: Job): Seq[CrawlerTaskEntity] = {
    val om: ObjectMapper = new ObjectMapper()
    val rootJsonNode = om.readTree(content)
    val itFields = rootJsonNode.getFieldNames
    val taskArrayBuffer = new ArrayBuffer[CrawlerTaskEntity]
    while (itFields.hasNext()) {
      val f = itFields.next()
      f.toLowerCase() match {
        case "jobname" => {
          job.jobName = rootJsonNode.get(f).getValueAsText
        }
        case "jobid" => {
          job.jobId = rootJsonNode.get(f).getValueAsLong
        }
        case _ => {
          val taskJsonNode = rootJsonNode.get(f)
          if (taskJsonNode.isArray()) {
            val iterator = taskJsonNode.iterator()
            while (iterator.hasNext()) {
              val jsonNode = iterator.next()
              taskArrayBuffer.append(getTaskEntityFromJson(jsonNode))
            }
          } else {
            taskArrayBuffer.append(getTaskEntityFromJson(taskJsonNode))
          }
        }
      }
    }
    // if jobName is null, reset with taskDomain
    if (StringUtils.isBlank(job.jobName)) {
      val taskOption = taskArrayBuffer.find { task => task != null }
      if (taskOption.nonEmpty) {
        val task = taskOption.get
        val domain = task.taskDomain
        job.jobName = s"job_$domain"
        task.jobName = job.jobName
      }
    }
    taskArrayBuffer.foreach { task => {
      if (task != null) {
        task.jobId = job.jobId
        task.jobName = job.jobName
        job.domainSet.add(task.taskDomain)
        //println(task)
      }
    }
    }
    taskArrayBuffer.filter { task => task != null }.toSeq
  }

  def getTaskEntityFromJson(jsonNode: JsonNode): CrawlerTaskEntity = {
    val itFields = jsonNode.getFieldNames
    val task = new CrawlerTaskEntity
    while (itFields.hasNext()) {
      val f = itFields.next()
      f.toLowerCase() match {
        case "taskuri" => {
          var url = jsonNode.get(f).getValueAsText;
          url = url.replaceAll("\\|", "%7c")
          task.taskURI = url
        }
        case "taskstarttime" => {
          val taskStartTime = jsonNode.get(f).getValueAsLong;
          task.taskStartTime = taskStartTime
        }
        case "tasktype" => {
          val taskType = jsonNode.get(f).getValueAsInt;
          task.taskType = CrawlerTaskType(taskType)
        }
        case "taskdomain" => {
          val taskDomain = jsonNode.get(f).getValueAsText;
          task.taskDomain = taskDomain
        }
        case "cookies" => {
          val cookies = jsonNode.get(f).getValueAsText;
          task.cookies = cookies
        }
        case "useragent" => {
          val userAgent = jsonNode.get(f).getValueAsText;
          task.userAgent = userAgent
        }
        case "totaldepth" => {
          val totalDepth = jsonNode.get(f).getValueAsInt;
          task.totalDepth = totalDepth
        }
        case "charset" => {
          val charset = jsonNode.get(f).getValueAsText;
          task.charset = charset
        }
        case "retrytime" => {
          val retryTime = jsonNode.get(f).getValueAsInt;
          task.retryTime = retryTime
        }
        case "isuseproxy" => {
          val isUseProxy = jsonNode.get(f).getValueAsInt.toShort;
          task.isUseProxy = isUseProxy
        }
        case "intervaltime" => {
          val intervalTime = jsonNode.get(f).getValueAsInt;
          task.intervalTime = intervalTime
        }
        case "httpmethod" => {
          val httpmethod = jsonNode.get(f).getValueAsInt;
          task.httpmethod = HttpRequestMethodType(httpmethod)
        }
        case "topcicrawlerparserclassname" => {
          val topciCrawlerParserClassName = jsonNode.get(f).getValueAsText;
          task.topciCrawlerParserClassName = topciCrawlerParserClassName
        }
        case "isneedsaveparseryslf" => {
          val isNeedSaveParserYslf = jsonNode.get(f).getValueAsInt;
          task.isNeedSaveParserYslf = NeedSaveParser(isNeedSaveParserYslf)
        }
        case "totalbatch" => {
          val totalBatch = jsonNode.get(f).getValueAsInt;
          task.totalBatch = totalBatch
        }
        case "httprefer" => {
          val httpRefer = jsonNode.get(f).getValueAsText;
          task.httpRefer = httpRefer
        }
        case "forbiddencode" => {
          val forbiddenCode = jsonNode.get(f).getValueAsText;
          task.forbiddenCode = forbiddenCode
        }
        case "taskip" => {
          val taskIp = jsonNode.get(f).getValueAsText;
          task.taskIp = taskIp
        }
        case "proxyhost" => {
          val proxyHost = jsonNode.get(f).getValueAsText;
          task.proxyHost = proxyHost
        }
        case "proxyport" => {
          val proxyPort = jsonNode.get(f).getValueAsInt;
          task.proxyPort = proxyPort
        }
        case "schemefile" => {
          val schemeFile = jsonNode.get(f).getValueAsText;
          task.schemeFile = schemeFile
        }
        case "keywordsofinvalid" => {
          val keyWordsOfInvalid = jsonNode.get(f).getValueAsText;
          task.keyWordsOfInvalid = keyWordsOfInvalid
        }

        case _ => {}
      }
    }
    /* if (CrawlerControlImpl.isValidUrl(task.taskURI) == false) {
       null
     } else {*/
    if (task.taskURI != null && !task.taskURI.equalsIgnoreCase(""))
      task.taskDomain = getDomain(task.taskURI, task.taskDomain)
    task
    // }
  }

  //add the seedTask and distribute the jobId
  def addSeedTask(
                   seed: String, taskType: CrawlerTaskType.Type = CrawlerTaskType.GENERAL_CRAWLER,
                   taskDomain: String = "", cookies: String = "",
                   jobName: String = "", userAgent: String = CrawlerTaskEntity.DefalutUserAgent,
                   charset: String = CrawlerTaskEntity.DefaultCharset,
                   retryTime: Int = 3, isUseProxy: Short = 0, intervalTime: Int = 0,
                   httpmethod: HttpRequestMethodType.Type = HttpRequestMethodType.GET,
                   topciCrawlerParserClassName: String = "null",
                   isNeedSaveParserYslf: NeedSaveParser.Door = NeedSaveParser.NO_NEED,
                   totalBatch: Int = 1, httpRefer: String = "null", totalDepth: Int = 1): Boolean = {
    val isValid = CrawlerControlImpl.isValidUrl(seed)
    if (isValid == false) {
      return false
    }
    val job = new Job(jobName)
    try {
      val taskEntity = createTask(job, seed, taskType, taskDomain, cookies, userAgent,
        charset, retryTime, isUseProxy, intervalTime, httpmethod, topciCrawlerParserClassName,
        isNeedSaveParserYslf, totalBatch, httpRefer, totalDepth)
      job.domainSet.add(taskEntity.taskDomain)
      submitTask(taskEntity)
    } catch {
      case t: Throwable => {
        logError("create CrawlerTaskEntity error", t)
        return false
      }
    }
  }

  private def createTask(
                          job: Job, url: String, taskType: CrawlerTaskType.Type,
                          taskDomain: String, cookies: String, userAgent: String,
                          charset: String, retryTime: Int, isUseProxy: Short,
                          intervalTime: Int, httpmethod: HttpRequestMethodType.Type,
                          topciCrawlerParserClassName: String, isNeedSaveParserYslf: NeedSaveParser.Door,
                          totalBatch: Int, httpRefer: String, totalDepth: Int): CrawlerTaskEntity = {
    val task = new CrawlerTaskEntity
    task.taskStartTime = System.currentTimeMillis() + 5000
    task.jobId = job.jobId
    task.jobName = job.jobName
    task.taskURI = url
    task.taskType = taskType
    task.taskDomain = getDomain(url, taskDomain)
    task.cookies = getCookiesByDomain(cookies, task.taskDomain)
    task.userAgent = userAgent
    task.totalDepth = totalDepth
    task.charset = charset
    task.retryTime = retryTime
    task.isUseProxy = isUseProxy
    task.intervalTime = intervalTime
    task.httpmethod = httpmethod
    task.topciCrawlerParserClassName = topciCrawlerParserClassName
    task.isNeedSaveParserYslf = isNeedSaveParserYslf
    task.totalBatch = totalBatch
    task.httpRefer = httpRefer
    // if jobName is null, reset with taskDomain
    if (StringUtils.isBlank(job.jobName)) {
      val domain = task.taskDomain
      job.jobName = s"job_$domain"
      task.jobName = job.jobName
    }
    task
  }

  //submit the task to queue for schedule
  def submitTask(task: CrawlerTaskEntity): Boolean = {
    //val f = taskQueue.putElementToSortedSet(Constant(conf).CRAWLER_TASK_SORTEDSET_KEY, task, task.taskStartTime)
    val f = conf.taskManager.submitTask(task,task.taskStartTime)
    logInfo("submitTask " + task.toString)
    f
  }

  // if domain is empty, then reset domain form url
  def getDomain(url: String, domain: String = ""): String = {
    if (StringUtils.isBlank(domain) || domain.trim().equalsIgnoreCase("null")) {
      new java.net.URL(url).getHost
    } else {
      domain
    }
  }

  // if cookies is empty, then reset cookies form Redis by domain
  def getCookiesByDomain(cookies: String, domain: String): String = {
    if (StringUtils.isBlank(domain)) {
      ""
    } else {
      cookies
    }
  }

  // add User-Agent to Redis
  override def addUserAgent(userAgent: String): Unit = {
    if (StringUtils.isNotBlank(userAgent)) {
      redis.putToSet(userAgentSetName, userAgent)
      logDebug(s"add user-agent [$userAgent] to userAgent Redis Set")
    }
  }

  def addTaskMeituanTakeoutMerchant(actor: ActorRef, size: Int, startIndex: Int, batchSize: Int, jobId: Long, jobName: String): String = {
    presetTaskMeituanTakeoutMerchant(actor, size, startIndex, batchSize, jobId, jobName)
  }

  def presetTaskMeituanTakeoutMerchant(actor: ActorRef, size: Int, startIndex: Int, batchSize: Int, jobId: Long, jobName: String): String = {
    val random = new scala.util.Random
    var totalSize = size
    if (totalSize < 1) {
      totalSize = 1
    }
    var sizePerBatch = batchSize
    if (sizePerBatch < 1) {
      sizePerBatch = 100
    }
    var pendingSize = totalSize
    var batchCounter = 0
    var taskUriSeq: Array[(String, Long)] = Array.empty
    var counter = 0
    val batchInterval = Constant(conf).CRAWLER_CONTROL_TASK_BATCH_INTERVAL
    var GroupSize = Constant(conf).CRAWLER_CONTROL_TASK_GROUP_SIZE
    var groupCounter = 0
    var nextIndex = startIndex
    var taskOrdinal: Int = 1
    while (pendingSize > 0) {
      var tempMax = nextIndex
      var tempMin = nextIndex
      if (pendingSize > GroupSize) {
        tempMax += GroupSize - 1
      } else {
        tempMax += pendingSize - 1
      }
      logInfo(s"nextIndex [$nextIndex] tempMax [$tempMax]")
      val tempList = (tempMin to tempMax).toSeq.toArray
      val tempJavaList = JavaConverters.mutableSeqAsJavaListConverter(tempList).asJava
      Collections.shuffle(tempJavaList)
      var tempMerchantIdBuffer = JavaConverters.asScalaBufferConverter(tempJavaList).asScala.toArray
      for (i <- tempList) {
        counter += 1
        val psedoPos = random.nextInt(20)
        val psedoTaskStartTime = System.currentTimeMillis() + batchInterval * batchCounter + random.nextInt(5000)
        val taskUriTemplate = "http://waimai.meituan.com/restaurant/@merchantId?pos=@pos"
        val taskUri = taskUriTemplate.replace("@merchantId", "" + i).replace("@pos", "" + psedoPos)
        logInfo(s"taskUri[$taskUri]")
        taskUriSeq = taskUriSeq.+:((taskUri, psedoTaskStartTime))
        if (counter % sizePerBatch == 0 || counter == totalSize) {
          logInfo("before drop taskStrSeq.length  " + taskUriSeq.length)
          logInfo(s"batchCounter [$batchCounter] ")
          /* taskUriSeq.foreach { x => {
             val task = createTaskEntityMeituanTakeoutMerchant(jobId, jobName, x._1, x._2, taskOrdinal);
             submitTask(task);
             taskOrdinal += 1
           }
           }*/

          var commitedTaskSet = taskUriSeq.filter { x => {
            val task = createTaskEntityMeituanTakeoutMerchant(jobId, jobName, x._1, x._2, taskOrdinal);
            val t = submitTask(task)
            if (t) taskOrdinal += 1
            t
          }
          }
          w.post(JobStarted(jobId.toString, jobName, commitedTaskSet.length))
          commitedTaskSet = null
          batchCounter += 1
          taskUriSeq = taskUriSeq.drop(counter)
          logInfo("taskStrSeq.length  " + taskUriSeq.length)
        }
      }
      logInfo(s"tempJavaList [$tempJavaList] ")
      nextIndex = tempMax + 1
      pendingSize -= GroupSize
      groupCounter += 1
    }
    val result = s"\n\tstatistics: \ntotalSize[$totalSize], \nGroupSize[$GroupSize], groupCounter[$groupCounter], " +
      s"\nbatchSize[$sizePerBatch], batchCounter[$batchCounter], \n  counter[$counter]"
    logInfo(result)
    result
  }

  private def createTaskEntityMeituanTakeoutMerchant(jobId: Long, jobName: String, taskUri: String, taskStartTime: Long, taskOrdinal: Int): CrawlerTaskEntity = {
    val task: CrawlerTaskEntity = new CrawlerTaskEntity
    task.jobId = jobId
    task.jobName = jobName
    task.taskURI = taskUri
    task.taskDomain = "waimai.meituan.com"
    task.taskStartTime = taskStartTime
    task.userAgent = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50"
    task.cookies = "IJSESSIONID=zd06978gz2tw15vbq7sgezkob; iuuid=C8A2B33D0704DA7534EA03CBDD18EAB86226BF08B8C7328759823791E00ED96F; " +
      "ioc=9WR_Nseo4gUCWi2YccBKfkFPudcpBC_GtEPlykst8YtqtuxQlwH9PNPz4aq_yomYIo0e2Buqyv7N5WrcnMnh1foigiD6dO-L71Rm-sfcle8; latlng=31.221034,121.535086,1440468152180;" +
      " webp=1; ci3=1; a2h=2; abt=1440468514.0%7CBDF; rvct=10; SID=soledaroq7meep0jlj4cldocb2; ci=1; i_extend=C_b1E864817395347069952_a3279692_c0Gimthomepagecategory11H__a100173__b1; " +
      "stick-qrcode=1; ignore-zoom=true; ppos=39.920069%2C116.44267; pposn=%E5%90%8D%E5%B1%8B%E9%93%81%E6%9D%BF%E7%83%A7%E8%87%AA%E5%8A%A9%E9%A4%90; SERV=mos; " +
      "LREF=aHR0cHM6Ly9tb3MubWVpdHVhbi5jb20vdXNlcnMvc2lnbmluZw==; ttgr=355883; rvd=703926; __mta=142933642.1440468546949.1440468555820.1440492013105.4; " +
      "__utma=211559370.1150074935.1440468900.1440469534.1440491874.3; __utmb=211559370.3.9.1440492011356; __utmc=211559370; __utmz=211559370.1440468900.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); " +
      "__utmv=211559370.|3=dealtype=171=1; REMOVE_ACT_ALERT=1; w_uuid=455e1afb-d6fc-4334-b324-8aea493719d1; " +
      "uuid=f7a92229e0da16a19474.1440468514.0.0.0; oc=qH4yJAJEHAFloXFyJ1zBxYo4Yhbm3kHbK1Ij7CMptdllVU66rIEslt5HAYXc7Bua2wiVnRvEUXmsM28qKsoQB0AuuvwZgpiRJaLA9xmgGTWB6s0Dj8D37Gwr" +
      "4VCd8VVP2Cy0XS-x3q36QJFzEwpTFb-dSTgTzVaXv7wYz8wc540; w_cid=110100; w_cpy_cn=\'%E5%8C%97%E4%BA%AC\'; w_cpy=beijing; " +
      "waddrname=\'%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96\'; w_geoid=wx4g1ypnct88; " +
      "w_ah=\'39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96\'; " +
      "w_utmz=\'utm_campaign=(direct)&utm_source=(direct)&utm_medium=(none)&utm_content=(none)&utm_term=(none)\'; " +
      "w_visitid=b734e00e-e955-4277-a2ef-ae9248315710; JSESSIONID=165wko5g8p9mj1b6uksx91wy1y; __mta=142933642.1440468546949.1440492013105.1440493436387.5"
    task.forbiddenCode = "429"
    task.taskType = CrawlerTaskType.THEME_CRAWLER
    task.topciCrawlerParserClassName = "com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser"
    task.isUseProxy = 1
    task.keyWordsOfInvalid = "offline"
    task.httpRefer = "http://waimai.meituan.com/home/wx4g1ypnct88"
    task.taskOrdinal = taskOrdinal
    task
  }

}

private[crawler] object CrawlerControlImpl extends Logging {
  //Map(jobId=>(docId=>SemanticNode))
  val semanticTreeMap = new HashMap[String, Map[String, SemanticNode]]()

  def apply(conf: CrawlerConf): CrawlerControl = {
    new CrawlerControlImpl(conf)
  }

  // check Url
  def isValidUrl(url: String): Boolean = {
    var isValid = false
    if (StringUtils.isBlank(url)) {
    } else {
      val urlTrimed = url.toLowerCase().trim()
      isValid = urlTrimed.matches("((http://)|(https://)){1}\\S+")
    }
    logDebug(s"validate url [$url]: [$isValid]")
    isValid
  }

}

object TestCrawlerController {

  def main(args: Array[String]): Unit = {
    /* for (i <- 0 to 0) {
       //      testAddTaskByJson2
       //testMeituanTakeoutMerchant
       getBaiduAddressCoordinate
     }*/

    getBaiduAddressCoordinate

  }

  def testAddTaskByJson = {
    val crawlerControl = CrawlerControl(new CrawlerConf())
    crawlerControl.addSeedTaskByJson("{\"jobname\":\"test_jobname\"," +
      "\"task\":{\"taskuri\":\"http://waimai.meituan.com/restaurant/212018?pos=33\"," +
      "\"cookies\":\"REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96'; w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\"," +
      "\"taskType\":0,\"totalDepth\":2,\"totalBatch\":1,\"userAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0\",\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser\",\"httpRefer\":\"http://waimai.meituan.com/home/wx4g1ypnct88\"}}")
  }

  def testAddTaskByJson2 = {
    val crawlerControl = CrawlerControl(new CrawlerConf())
    crawlerControl.addSeedTaskByJson("{\"jobname\":\"waimai.meituan\"," +
      "\"task\":[{\"taskuri\":\"http://waimai.meituan.com/restaurant/212018?pos=33\"," +
      "\"cookies\":\"REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96'; w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\"," +
      "\"taskType\":0,\"totalDepth\":2,\"totalBatch\":1,\"userAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0\",\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser\",\"httpRefer\":\"http://waimai.meituan.com/home/wx4g1ypnct88\"}," +
      "{\"taskuri\":\"http://waimai.meituan.com/restaurant/212001?pos=13\"," +
      "\"cookies\":\"REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96'; w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\"," +
      "\"taskType\":0,\"totalDepth\":2,\"totalBatch\":1,\"userAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0\",\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser\",\"httpRefer\":\"http://waimai.meituan.com/home/wx4g1ypnct88\"}," +
      "{\"taskuri\":\"http://waimai.meituan.com/restaurant/213031?pos=19\"," +
      "\"cookies\":\"REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96'; w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\"," +
      "\"taskType\":0,\"totalDepth\":2,\"totalBatch\":1,\"userAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0\",\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser\",\"httpRefer\":\"http://waimai.meituan.com/home/wx4g1ypnct88\"}]}")
  }

  def testAddUserAgent = {
    val crawlerControl = CrawlerControl(new CrawlerConf())
    crawlerControl.addUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.99 Safari/537.36 LBBROWSER")
  }

  def getBaiduAddressCoordinate: Unit = {
    //    val shopsURL = "http://waimai.baidu.com/mobile/waimai?qt=shoplist&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&page=1&display=json"
    val addressResolveURLTemplate = "http://waimai.baidu.com/waimai?qt=poisearch&ie=utf-8&sug=0&tn=B_NORMAL_MAP&oue=1&res=1&display=json&wd="
    val collSize = MongoStorage.getCollectionSize(classOf[TakeoutMerchant])
    val limit = 20
    val fetchCount = if (collSize % limit == 0) collSize / limit else collSize / limit + 1
    val addressInfoList = MongoStorage.getList(classOf[AddressInfo], "address")
    for (i <- 0 until fetchCount.toInt) {

      //      val takeoutEateryList = MongoStorage.getListByValue(classOf[TakeoutMerchant], "or", "waimai.meituan.com", "address")
      val takeoutEateryList = MongoStorage.getListLimitedByValue(classOf[TakeoutMerchant], "originSite", "waimai.meituan.com", i * limit, limit, "address")
      var isFound = false
      var count = 0
      for (takeoutEatery <- takeoutEateryList) {
        isFound = false
        breakable {
          for (addressInfo <- addressInfoList) {
            if (takeoutEatery.getAddress.equals(addressInfo.address)) {
              isFound = true
              break
            }
          }
        }
        if (!isFound) {
          count += 1
          val addressResolveURL = addressResolveURLTemplate + takeoutEatery.getAddress
          CrawlerControl(new CrawlerConf()).addSeedTask(addressResolveURL, taskType = CrawlerTaskType.THEME_CRAWLER, cookies = "", totalDepth = 1,
            topciCrawlerParserClassName = "com.foofv.crawler.parse.topic.BaiduTakeoutEateryParser", jobName = "baidu:address-coordinate")
        }
        //      if (count == 5) {
        //        println("count= " + count)
        //        return
        //      }
      }
      println("count= " + count)
    }
  }

  def getBaiduTakeoutEateries = {

    val shopsURLTemplate = "http://waimai.baidu.com/mobile/waimai?qt=shoplist&address=&lat=&lng=&page=1&display=json"
    val addressInfoList = MongoStorage.getAll(classOf[AddressInfo])
    for (addressInfo <- addressInfoList) {
      val shopsURL = shopsURLTemplate.replaceAll("address=", "address=" + addressInfo.address).
        replaceAll("lat=", "lat=" + addressInfo.latitude).
        replaceAll("lng=", "lng=" + addressInfo.longitude)
      CrawlerControl(new CrawlerConf()).addSeedTask(shopsURL, taskType = CrawlerTaskType.THEME_CRAWLER, cookies = "", totalDepth = 2,
        topciCrawlerParserClassName = "com.foofv.crawler.parse.topic.BaiduTakeoutEateryParser", jobName = "baidu:shops")
    }
  }

  def test = {
    val crawlerControl = CrawlerControl(new CrawlerConf())
    crawlerControl.addSeedTask("http://www.baidu.com/?tn=10018801_hao")
  }

  def testRedisClient = {
    val crawlerControl = CrawlerControl(new CrawlerConf())
    crawlerControl.addSeedTask("http://waimai.meituan.com/restaurant/177274", cookies = "REMOVE_ACT_ALERT=1", taskType = CrawlerTaskType.THEME_CRAWLER)
  }

  def testMeituanTakeoutMerchant = {
    val crawlerControl = CrawlerControl(new CrawlerConf())
    crawlerControl.addSeedTask("http://waimai.meituan.com/restaurant/212018?pos=12",
      cookies = "REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; w_cpy_cn=\"%E5%8C%97%E4%BA%AC\"; w_cpy=beijing; waddrname=\"%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96\"; w_geoid=wx4g1ypnct88; w_ah=\"39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93\"; w_utmz=\"utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)\"; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20",
      taskType = CrawlerTaskType.THEME_CRAWLER,
      totalDepth = 2,
      totalBatch = 1,
      userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0",
      topciCrawlerParserClassName = "com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser",
      httpRefer = "http://waimai.meituan.com/home/wx4g1ypnct88")
  }

  def testMeituanCityList = {
    val crawlerControl = CrawlerControl(new CrawlerConf())
    crawlerControl.addSeedTask("http://www.meituan.com/index/changecity/initiative?mtt=1.index%2Ffloornew.0.0.iccw5k3k",
      cookies = "__mta=51753494.1437455943994.1437455943994.1437455943994.1; ci=1; abt=1437455917.0%7CADE; SID=lcfbgl692nco1s3u6fvlk9esr0; ignore-zoom=true; rvct=1; nodelogin=1; __utma=211559370.712368698.1437455944.1437455944.1437455944.1; __utmb=211559370.3.9.1437456031899; __utmc=211559370; __utmz=211559370.1437455944.1.1.utmcsr=baidu|utmccn=baidu|utmcmd=organic|utmcct=homepage; __utmv=211559370.|1=city=bj=1; uuid=71d36594270e77336f46.1437455917.0.0.0; oc=U02cY8_JAshtF9qi_kOGzwcc7PmP7XwQRwAgVjk8c8UYpKxYg41iYkz8LuQTmEOrrBjeHU_gY8OBdblAI_bn8szXgNMfLs07lpyaU3fkra-GQEZ9-xbVWPPm9FgLb2Om_Ui8lMMsLjrBJjI91OPO2CMmxKoUM6VVhT0IRKS1GZY",
      taskType = CrawlerTaskType.THEME_CRAWLER,
      totalDepth = 2,
      topciCrawlerParserClassName = "com.foofv.crawler.parse.topic.MeituanCityListTopicParser",
      httpRefer = "http://bj.meituan.com/?utm_campaign=baidu&utm_medium=organic&utm_source=baidu&utm_content=homepage&utm_term=")
  }

  def testIsValideUrl = {
    println(CrawlerControlImpl.isValidUrl("http://www.baidu.com/"))
    println(CrawlerControlImpl.isValidUrl("http://w1"))
    println(CrawlerControlImpl.isValidUrl("http://qweqwe.qw/eqwe123123/12321.html"))
  }

}
