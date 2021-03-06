package com.foofv.crawler.entity

import java.io.InputStream
import java.util.UUID

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import com.foofv.crawler.enumeration.{CrawlerTaskFetchStatus, CrawlerTaskRuleUpdateType, CrawlerTaskType, HttpRequestMethodType}
import redis.ByteStringFormatter
import akka.util.ByteString
import com.foofv.crawler.enumeration.NeedSaveParser
import com.foofv.crawler.enumeration.StoragePlugin
import org.apache.commons.beanutils.BeanUtils
import java.util.regex.Pattern
import javax.swing.JPopupMenu.Separator

import scala.actors.threadpool.AtomicInteger

/**
  * @author soledede
  * @email wengbenjue@163.com
  *        taskEntity abstract
  *
  */
private[crawler] class CrawlerTaskEntity(
                                          var taskId: String = CrawlerTaskEntity.geneTaskId, // every crawlerTaskEntity have unique taskId, Not Null
                                          var parentTaskId: String = "null", // default "null"
                                          var parentTaskToParames: String = "null", // default "null", maybe you need give the url some parameter,because some webpage need it for search
                                          var jobId: Long = -1, // auto increment, Not Null
                                          var jobName: String = "", // default s"job-$jobId", can be specified by user
                                          var currentBatchId: Int = 1, // default 0, current batch id
                                          var taskType: CrawlerTaskType.Type = CrawlerTaskType.GENERAL_CRAWLER, // default CrawlerTaskType.GENERAL_CRAWLER, specified by user
                                          var taskURI: String = "", // specified by user, ignore blank, Not Null
                                          var taskDomain: String = "null", // specified by user, destination host domain
                                          var cookies: String = "null", // default " ", http request cookies, specified by user
                                          var userAgent: String = CrawlerTaskEntity.DefalutUserAgent, // default " ", can be specified by user
                                          var charset: String = CrawlerTaskEntity.DefaultCharset, // default "utf-8", can be specified by user
                                          var fetchStatus: CrawlerTaskFetchStatus.Status = CrawlerTaskFetchStatus.FETCHING, // default0, 0 unfetch, 1 fetched
                                          var retryTime: Int = 3, // default 3, can be specified by user
                                          var isUseProxy: Short = 1, // default 0, can be specified by user , 0 no 1 yes
                                          var ruleUpdateType: CrawlerTaskRuleUpdateType.Type = CrawlerTaskRuleUpdateType.AUTO_INTELLIGENCE, //default 0, can be specified by user, 0 auto intelligence,1 fixed time,2 machine learning
                                          var totalDepth: Int = 1, // default 1, can be specified by user
                                          var lastFetchStartTime: Long = -1, // default -1
                                          var lastFetchFinishedTime: Long = -1, // default -1
                                          var intervalTime: Int = 30, // default 30, timeunit minute, if CrawlerJobType.THEME_CRAWLER, can be specified by user, how often should we fetch the url
                                          var taskStartTime: Long = System.currentTimeMillis(), // default System.currentTimeMillis(), task start time
                                          var taskFinishTime: Long = -1, // default -1, task end time
                                          var taskCostTime: Long = -1, // default -1
                                          var httpmethod: HttpRequestMethodType.Type = HttpRequestMethodType.GET, // default HttpRequestMethodType.GET, HTTP request method, get or post
                                          var topciCrawlerParserClassName: String = "null", // default " ", if CrawlerJobType.THEME_CRAWLER, specified by user
                                          var isNeedSaveParserYslf: NeedSaveParser.Door = NeedSaveParser.NO_NEED, //if need save the parser object by yourself,if set NeedSaveParser.NO_NEED,we will save it by system
                                          //var crawlerStorage: String        // you can choice which storage fro crawler is used
                                          var currentDepth: Int = 1, // default 0, current depth
                                          var totalBatch: Int = 1, // default 1, total batch
                                          var taskOrdinal: Int = 1, // the ordinal number of tasks at the same depth, starting at one
                                          var httpRefer: String = "null", // default "", HTTP header refer
                                          var currentDepthCompleted: String = "true", // default "true"
                                          var forbiddenCode: String = "null", //if you set this value,we will filter all response code page except 200 and forbiddenCode
                                          var schemeDocId: String = "null", //if need use scheme,we will allocate a document id for every web page
                                          var schemeFile: String = "null",
                                          var userAgentType: Int = 1,
                                          var taskIp: String = "null",
                                          var proxyHost: String = "null",
                                          var proxyPort: Int = -1,
                                          var keyWordsOfInvalid: String = "null",
                                          var contextJsonString: String = "null",
                                          var tableName: String = "null",
                                          var isStream: Int = 0,
                                          var storage: String = "mongodb",
                                          var originType: String = "pc" // pc or phone
                                        ) extends Serializable with Cloneable {

  override def toString(): String = {
    ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE)
  }


  def allCloneSelf(): CrawlerTaskEntity = {
    val task = new CrawlerTaskEntity
    task.parentTaskId = this.parentTaskId
    task.parentTaskToParames = this.parentTaskToParames
    task.jobId = this.jobId
    task.jobName = this.jobName
    task.currentBatchId = this.currentBatchId
    task.taskType = this.taskType
    task.taskURI = this.taskURI
    task.taskDomain = this.taskDomain
    task.cookies = this.cookies
    task.userAgent = this.userAgent
    task.charset = this.charset
    task.fetchStatus = this.fetchStatus
    task.retryTime = this.retryTime
    task.isUseProxy = this.isUseProxy
    task.ruleUpdateType = this.ruleUpdateType
    task.totalDepth = this.totalDepth
    task.lastFetchStartTime = this.lastFetchStartTime
    task.lastFetchFinishedTime = this.lastFetchFinishedTime
    task.intervalTime = this.intervalTime
    task.taskStartTime = this.taskStartTime
    task.taskFinishTime = this.taskFinishTime
    task.taskCostTime = this.taskCostTime
    task.httpmethod = this.httpmethod
    task.topciCrawlerParserClassName = this.topciCrawlerParserClassName
    task.isNeedSaveParserYslf = this.isNeedSaveParserYslf
    task.totalBatch = this.totalBatch
    task.taskOrdinal = this.taskOrdinal
    task.httpRefer = this.httpRefer
    task.currentDepth = this.currentDepth
    task.forbiddenCode = this.forbiddenCode
    task.currentDepthCompleted = this.currentDepthCompleted
    task.schemeDocId = this.schemeDocId
    task.schemeFile = this.schemeFile
    task.userAgentType = this.userAgentType
    task.taskIp = this.taskIp
    task.proxyHost = this.proxyHost
    task.proxyPort = this.proxyPort
    task.keyWordsOfInvalid = this.keyWordsOfInvalid
    task.contextJsonString = this.contextJsonString
    task.tableName = this.tableName
    task.isStream = this.isStream
    task.storage = this.storage
    task.originType = this.originType
    /* task.schemaItelligent = this.schemaItelligent
     task.schemaRefUrl = this.schemaRefUrl
     task.schemaPreDocId = this.schemaPreDocId
     task.schemaPreUrl = this.schemaPreUrl
     task.schemaRefDocId = this.schemaRefDocId*/
    task
  }


  override def clone(): CrawlerTaskEntity = {
    val task = new CrawlerTaskEntity
    task.jobId = this.jobId
    task.jobName = this.jobName
    task.currentBatchId = this.currentBatchId
    task.taskType = this.taskType
    task.taskURI = this.taskURI
    task.taskDomain = this.taskDomain
    task.cookies = this.cookies
    task.userAgent = this.userAgent
    task.charset = this.charset
    task.fetchStatus = this.fetchStatus
    task.retryTime = this.retryTime
    task.isUseProxy = this.isUseProxy
    task.ruleUpdateType = this.ruleUpdateType
    task.totalDepth = this.totalDepth
    task.intervalTime = this.intervalTime
    task.httpmethod = this.httpmethod
    task.topciCrawlerParserClassName = this.topciCrawlerParserClassName
    task.isNeedSaveParserYslf = this.isNeedSaveParserYslf
    task.totalBatch = this.totalBatch
    task.httpRefer = this.taskURI
    task.currentDepth = this.currentDepth
    task.forbiddenCode = this.forbiddenCode
    task.schemeDocId = this.schemeDocId
    task.schemeFile = this.schemeFile
    task.userAgentType = this.userAgentType
    task.taskIp = this.taskIp
    task.proxyHost = this.proxyHost
    task.proxyPort = this.proxyPort
    task.keyWordsOfInvalid = this.keyWordsOfInvalid
    task.contextJsonString = this.contextJsonString
    task.tableName = this.tableName
    task.isStream = this.isStream
    task.storage = this.storage
    task.originType = this.originType
    /*task.schemaItelligent = this.schemaItelligent
    task.schemaPreDocId = this.schemaPreDocId
    task.schemaPreUrl = this.schemaPreUrl
    task.schemaRefDocId = this.schemaRefDocId*/
    task
  }


  def geneChildTaskEntity(): CrawlerTaskEntity = {
    val task = this.clone()
    task.httpRefer = this.taskURI
    task.parentTaskId = this.taskId
    task
  }

  def geneSiblingTaskEntity(): CrawlerTaskEntity = {
    val task = this.clone()
    task.httpRefer = this.taskURI
    task.parentTaskId = this.parentTaskId
    //task.currentDepth = task.currentDepth - 1
    task
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[CrawlerTaskEntity]

  override def equals(other: Any): Boolean = other match {
    case that: CrawlerTaskEntity =>
      (that canEqual this) &&
        taskId == that.taskId
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(taskId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object CrawlerTaskEntity {
  private val Separator = "#&#&#"
  implicit val byteStringFormatter = new ByteStringFormatter[CrawlerTaskEntity] {
    def serialize(task: CrawlerTaskEntity): ByteString = {
      ByteString(
        task.taskId + Separator +
          task.parentTaskId + Separator +
          task.parentTaskToParames + Separator +
          task.jobId + Separator +
          task.jobName + Separator +
          task.currentBatchId + Separator +
          task.taskType.id + Separator +
          task.taskURI + Separator +
          task.taskDomain + Separator +
          task.cookies + Separator +
          task.userAgent + Separator +
          task.charset + Separator +
          task.fetchStatus.id + Separator +
          task.retryTime + Separator +
          task.isUseProxy + Separator +
          task.ruleUpdateType.id + Separator +
          task.totalDepth + Separator +
          task.lastFetchStartTime + Separator +
          task.lastFetchFinishedTime + Separator +
          task.intervalTime + Separator +
          task.taskStartTime + Separator +
          task.taskFinishTime + Separator +
          task.taskCostTime + Separator +
          task.httpmethod.id + Separator +
          task.topciCrawlerParserClassName + Separator +
          task.isNeedSaveParserYslf.id + Separator +
          //task.crawlerStorage
          task.currentDepth + Separator +
          task.totalBatch + Separator +
          task.taskOrdinal + Separator +
          task.httpRefer + Separator +
          task.currentDepthCompleted + Separator +
          task.forbiddenCode + Separator +
          task.schemeDocId + Separator +
          task.schemeFile + Separator +
          task.userAgentType + Separator +
          task.taskIp + Separator +
          task.proxyHost + Separator +
          task.proxyPort + Separator +
          task.keyWordsOfInvalid + Separator +
          task.contextJsonString + Separator +
          task.tableName + Separator +
          task.isStream + Separator +
          task.storage + Separator +
          task.originType
      )
    }

    def deserialize(bs: ByteString): CrawlerTaskEntity = {
      val r = bs.utf8String.split(Pattern.quote(Separator)).toList
      val task = new CrawlerTaskEntity()
      task.taskId = r(0)
      task.parentTaskId = r(1)
      task.parentTaskToParames = r(2)
      task.jobId = r(3).toLong
      task.jobName = r(4)
      task.currentBatchId = r(5).toInt
      task.taskType = CrawlerTaskType(r(6).toInt)
      task.taskURI = r(7)
      task.taskDomain = r(8)
      task.cookies = r(9)
      task.userAgent = r(10)
      task.charset = r(11)
      task.fetchStatus = CrawlerTaskFetchStatus(r(12).toInt)
      task.retryTime = r(13).toInt
      task.isUseProxy = r(14).toShort
      task.ruleUpdateType = CrawlerTaskRuleUpdateType(r(15).toInt)
      task.totalDepth = r(16).toInt
      task.lastFetchStartTime = r(17).toLong
      task.lastFetchFinishedTime = r(18).toLong
      task.intervalTime = r(19).toInt
      task.taskStartTime = r(20).toLong
      task.taskFinishTime = r(21).toLong
      task.taskCostTime = r(22).toLong
      task.httpmethod = HttpRequestMethodType(r(23).toInt)
      task.topciCrawlerParserClassName = r(24)
      task.isNeedSaveParserYslf = NeedSaveParser(r(25).toInt)
      task.currentDepth = r(26).toInt
      task.totalBatch = r(27).toInt
      task.taskOrdinal = r(28).toInt
      task.httpRefer = r(29)
      task.currentDepthCompleted = r(30)
      task.forbiddenCode = r(31)
      task.schemeDocId = r(32)
      task.schemeFile = r(33)
      task.userAgentType = r(34).toInt
      task.taskIp = r(35)
      task.proxyHost = r(36)
      task.proxyPort = r(37).toInt
      task.keyWordsOfInvalid = r(38)
      task.contextJsonString = r(39)
      task.tableName = r(40)
      task.isStream = r(41).toInt
      task.storage = r(42)
      task.originType = r(43)
      task
    }
  }

  val DefalutUserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.99 Safari/537.36 LBBROWSER"
  val DefaultCharset = "UTF-8"

  val taskCounter = new AtomicInteger(1)

  def geneTaskId = java.util.UUID.randomUUID().toString() + "#" + taskCounter.getAndIncrement


}
