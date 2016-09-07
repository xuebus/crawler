/**
  * Copyright [2015] [soledede]
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.foofv.crawler.parse

import com.foofv.crawler.storage.StorageManager
import com.foofv.crawler.storage.StorageManagerFactory
import com.foofv.crawler.util.Logging
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.rule.ITaskFilter
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.enumeration.NeedSaveParser
import com.foofv.crawler.schedule.ITaskQueueFactory
import com.foofv.crawler.rule.ITaskFilterFactory
import com.foofv.crawler.parse.util.Link
import com.foofv.crawler.enumeration._
import com.foofv.crawler.blockqueue.CrawlerBlockQueue
import com.foofv.crawler.util.constant.Constant
import com.foofv.crawler.util.listener.{JobTaskCompleted, JobTaskAdded, ManagerListenerWaiter}

/**
  * @author soledede
  */
private[crawler] class ParserManager private(conf: CrawlerConf) extends Logging with ParserFactory with ITaskFilterFactory with URLAssemblerFactory with URLDistinctManagerFactory with ITaskQueueFactory {

  //var analyzer: Analyzer = createAnalyzer()
  val w = ManagerListenerWaiter()
  var taskFilter: ITaskFilter = _
  var urlAssembler: URLAssembler = _
  var urllDistinctManager: URLDistinctManager = _
  var taskQueue: ITaskQueue = createTaskQueue
  val parserObjAsynCacheQueue = CrawlerBlockQueue[(Parser, ResObj, AnyRef)]("linkedBlock", "asynParserObj", Int.MaxValue, conf)
  private val childTaskScoreMaxLimit = conf.getInt("crawler.parsermanager.childtask.submit.range.max.limit", 20)
  private val childTaskSubmitInterval = conf.getInt("crawler.parsermanager.childtask.submit.interval", 10)
  //default 3 seconds
  private val childTaskSubmitBatchSize = conf.getInt("crawler.parsermanager.childtask.submit.batch.size", 10)
  //default 10
  private val taskSortedSetName = Constant(conf).CRAWLER_TASK_SORTEDSET_KEY
  private val random = new java.util.Random()

  def start(resObj: ResObj) = {
    try {

      if (resObj != null) {
        val parser = currentParser(resObj, conf) //create parser object  single instance
        if (parser != null) {
          w.post(JobTaskCompleted(resObj.tastEntity.jobId.toString, resObj.tastEntity.jobName, 1))
          checkDepth(resObj)
          var obj: AnyRef = null
          // parser filter urls
          var parserFilterTaskEntitySet: Seq[CrawlerTaskEntity] = null
          var newTaskEntitySet: (Seq[CrawlerTaskEntity], AnyRef) = null
          try {
            if (resObj.tastEntity.isStream != 1)
              newTaskEntitySet = parser.geneNewTaskEntitySet(resObj)
          } catch {
            case e: Exception => logError("geneNewTaskEntitySet failed!", e)
          }

          if (newTaskEntitySet != null) obj = newTaskEntitySet._2
          //val subCrawlerTaskEntitySet = parser.extractSubCrawlerTaskEntitySet(resObj)
          if (newTaskEntitySet != null && newTaskEntitySet._1 != null)
            parserFilterTaskEntitySet = newTaskEntitySet._1
          // else if (newTaskEntitySet == null) parserFilterTaskEntitySet = subCrawlerTaskEntitySet._1
          //else if (subCrawlerTaskEntitySet == null) parserFilterTaskEntitySet = subCrawlerTaskEntitySet._1

          if (parserFilterTaskEntitySet != null && !isLastDepth(resObj.tastEntity)) {
            resObj.tastEntity.taskType match {
              case CrawlerTaskType.GENERAL_CRAWLER => then(uniqueCrawlerTaskEntitys(parserFilterTaskEntitySet), resObj) //remove double urls if it's general crawler
              case _ => then(parserFilterTaskEntitySet, resObj)
            }
          }
          // isNeedSaveParserYslf is for user parser and save object by himself/herself     change to asyn parser and persist in ParserPersistRunner
          /* resObj.tastEntity.isNeedSaveParserYslf match {
          case NeedSaveParser.NO_NEED => saveEntity(parser.parse(resObj))
          case NeedSaveParser.NEED    => parser.parseNotSave(resObj)
        }*/

          // if CrawlerJobType.THEME_CRAWLER && currentDepth == 0 && currentBatchId < totalBatchCount
          // update TaskEntity currentBatchId ,put TaskEntity back to Sortedset and start next batch
          putTaskEntityBacktoSortedSet(resObj.tastEntity)
          //parser Content with other thread Pool, we use asynchronous ,parser content and persist parser object (liking save to mongoDB)
          //(obj!=null) {
          if (obj != null) {
            if (obj.isInstanceOf[(String, AnyRef)]) {
              if (obj.asInstanceOf[(String, AnyRef)]._2 != null) {
                if (parserObjAsynCacheQueue.offer((parser, resObj, obj))) logDebug("Parser ResObj put queue sucecessful!") else logWarning("Parser ResObj put queue fail!")
              }
            }
          } else {
            if (parserObjAsynCacheQueue.offer((parser, resObj, obj))) logDebug("Parser ResObj put queue sucecessful!") else logWarning("Parser ResObj put queue fail!")
          }

          //}
        }
      }
    } catch {
      case t: Throwable => logError(s"parser manager excute failed", t)
    }

    def then(taskEntitySet: Seq[CrawlerTaskEntity], resObj: ResObj) = {
      val depCrawlerTaskEntityList = setDepth(taskEntitySet)
      if (depCrawlerTaskEntityList != null && depCrawlerTaskEntityList.size > 0) {
        val filterCrawlerTaskEntityList = filterUrls(taskEntitySet) // filter urls 
        if (filterCrawlerTaskEntityList != null && filterCrawlerTaskEntityList.size > 0) {
          val taskEntityList: Seq[CrawlerTaskEntity] = assembleUrl(filterCrawlerTaskEntityList, resObj) // assemble url and return CrawlerTaskEntity
          if (taskEntityList != null && taskEntityList.size > 0) {
            schedule(taskEntityList, resObj)
          }
        }
      }
    }
  }


  private def checkDepth(resObj: ResObj): Unit = {
    if (!resObj.tastEntity.currentDepthCompleted.toBoolean) resObj.tastEntity.currentDepth -= 1
  }

  private def uniqueCrawlerTaskEntitys(taskEntitySet: Seq[CrawlerTaskEntity]): Seq[CrawlerTaskEntity] = {
    //TO DO
    null
  }

  private def filterUrls(taskEntitySet: Seq[CrawlerTaskEntity]): Seq[CrawlerTaskEntity] = {
    // TODO 
    taskEntitySet
  }

  private def setDepth(taskEntitySet: Seq[CrawlerTaskEntity]): Seq[CrawlerTaskEntity] = {
    taskEntitySet.map {
      c =>
        c.currentDepth = c.currentDepth + 1
        c
    }
  }

  private def assembleUrl(taskEntitySet: Seq[CrawlerTaskEntity], resObj: ResObj): Seq[CrawlerTaskEntity] = {
    // TODO 
    taskEntitySet
  }

  // put CrawlerJobType.THEME_CRAWLER TaskEnity back to SortedSet
  // currentDepth == 1
  private def putTaskEntityBacktoSortedSet(taskEntity: CrawlerTaskEntity) {
    var taskId = taskEntity.taskId
    val taskDepth = taskEntity.currentDepth
    var taskBatchId = taskEntity.currentBatchId
    val totalBatch = taskEntity.totalBatch
    taskEntity.taskType match {
      case CrawlerTaskType.GENERAL_CRAWLER => logInfo(s"GENERAL_CRAWLER TaskEntity[$taskId] need not to back SortedSet.")
      case CrawlerTaskType.THEME_CRAWLER => {
        if (taskEntity.currentDepth == 1 && taskBatchId < totalBatch) {
          // next batch task 
          val newTask = taskEntity.clone()
          taskId = newTask.taskId
          newTask.currentBatchId = taskEntity.currentBatchId + 1
          taskBatchId = newTask.currentBatchId
          //taskQueue.putElementToSortedSet(taskSortedSetName, newTask, System.currentTimeMillis() + newTask.intervalTime * 60000)
          conf.taskManager.submitTask(newTask, newTask.intervalTime * 60000)
          logInfo(s"task id[$taskId] depth[$taskDepth] batchId[$taskBatchId] totalBatch[$totalBatch] back to SortedSet and next batch start.")
        } else {
          logInfo(s"task id[$taskId] depth[$taskDepth]  batchId[$taskBatchId] totalBatch[$totalBatch] need not to back SortedSet.")
        }
      }
      case _ => logInfo("unknown TaskEntity: " + taskEntity)
    }
  }

  // put child-TaskEnity to SortedSet 
  def schedule(taskEntityList: Seq[CrawlerTaskEntity], resObj: ResObj) = {
    var batch: Int = 0
    var counter: Int = 1

    var commitTaskEntitySeq = taskEntityList.filter { taskEntity =>
      var flag: Boolean = false
      var commit: Boolean = false

      try {
        taskEntity.taskURI = taskEntity.taskURI.replaceAll("\\|", "%7c")
      }
      catch {
        case e: Exception => println("parent task:" + resObj.tastEntity.taskURI + "\n" + taskEntity.taskURI)
      }
      if (taskEntity.taskURI.trim.equalsIgnoreCase(resObj.tastEntity.taskURI.trim)) flag = true
      if (!flag) {
        if (counter % childTaskSubmitBatchSize == 0) {
          batch += 1
        }
        val offset = childTaskSubmitInterval * batch + random.nextInt(childTaskScoreMaxLimit * 1000) + 1
        //val t = taskQueue.putElementToSortedSet(taskSortedSetName, taskEntity, System.currentTimeMillis() + offset)
        val t = conf.taskManager.submitTask(taskEntity, System.currentTimeMillis() + offset)
        val taskId = taskEntity.taskId
        val taskUrl = taskEntity.taskURI
        logInfo(s"task[$taskId] url[$taskUrl] offset [$offset]")
        logDebug("submit child-TaskEnity to Redis.SortedSet " + taskEntity)
        if (t) {
          counter += 1
          commit = true
        }
      }
      commit
    }
    w.post(JobTaskAdded(resObj.tastEntity.jobId.toString, resObj.tastEntity.jobName, commitTaskEntitySeq.size))
    commitTaskEntitySeq = null

    /*    for (taskEntity <- taskEntityList) {
          var flag: Boolean = false
          taskEntity.taskURI = taskEntity.taskURI.replaceAll("\\|", "%7c")
          if (taskEntity.taskURI.trim.equalsIgnoreCase(resObj.tastEntity.taskURI.trim)) flag = true
          if (!flag) {
            if (counter % childTaskSubmitBatchSize == 0) {
              batch += 1
            }
            val offset = childTaskSubmitInterval * batch + random.nextInt(childTaskScoreMaxLimit * 1000) + 1
            taskQueue.putElementToSortedSet(taskSortedSetName, taskEntity, System.currentTimeMillis() + offset)
            val taskId = taskEntity.taskId
            val taskUrl = taskEntity.taskURI
            logInfo(s"task[$taskId] url[$taskUrl] offset [$offset]")
            logDebug("submit child-TaskEnity to Redis.SortedSet " + taskEntity)
            counter += 1
          }
        }*/
  }

  // check TaskEntity depth state.
  // if currentDepth == totalDepth, return true. This is the LAST depth and no child-TaskEntitys submit.
  // else return false
  private def isLastDepth(taskEntity: CrawlerTaskEntity): Boolean = {
    logInfo("task id[%s] has reached depth[%d] and catchDepth is [%d] ".format(taskEntity.taskId, taskEntity.currentDepth, taskEntity.totalDepth))
    taskEntity.currentDepth == taskEntity.totalDepth && (taskEntity.currentDepthCompleted.equalsIgnoreCase("true") || taskEntity.currentDepthCompleted.toBoolean == true)
  }

  //change to asyn parser and persist in ParserPersistRunner
  /* def saveEntity(entity: AnyRef): Boolean = {
    if (entity == null) return false
    // TODO 
    false
  }*/

  override def currentParser(resObj: ResObj, conf: CrawlerConf): Parser = {
    Parser.getParser(resObj.tastEntity, conf)

  }

  /*override def createAnalyzer(): Analyzer = {
    Analyzer("url", conf)
  }*/

  override def createITaskFilter(): ITaskFilter = {
    // TODO 
    null.asInstanceOf[ITaskFilter]
  }

  override def ceateURLAssembler(): URLAssembler = {
    // TODO 
    null.asInstanceOf[URLAssembler]
  }

  override def createUrlDistinctManager(): URLDistinctManager = {
    // TODO 
    null.asInstanceOf[URLDistinctManager]
  }

  override def createTaskQueue(): ITaskQueue = {
    ITaskQueue("sortSet", conf)
  }

}

object ParserManager {
  var parserManager: ParserManager = null

  def apply(conf: CrawlerConf) = {
    if (parserManager == null) parserManager = new ParserManager(conf)
    parserManager
  }
}

object TestParserManager {
  def main(args: Array[String]): Unit = {
    testSchedule
  }

  def testSchedule = {
    val conf = new CrawlerConf
    val taskEntityList = Seq(testTaskEntity, testTaskEntity, testTaskEntity, testTaskEntity)
    val parserManager = ParserManager(conf)
    val resObj: ResObj = new ResObj(testTaskEntity, null)
    parserManager.schedule(taskEntityList, resObj)
  }

  def testParserManagerStart = {
    val conf = new CrawlerConf
    val taskEntity = testTaskEntity
    val resObj: ResObj = new ResObj(taskEntity, null)
    val parserManager = ParserManager(conf)
    parserManager.start(resObj)
    parserManager.start(resObj)
    parserManager.start(resObj)
    parserManager.start(resObj)
    parserManager.start(resObj)
  }

  def testParserCacheManager = {
    val conf = new CrawlerConf
    val taskEntity = testTaskEntity
    val resObj: ResObj = new ResObj(taskEntity, null)
    val parser = ParserManager(conf).currentParser(resObj, new CrawlerConf)
    println(parser)
    Parser.setParserInstanceInvalide(taskEntity.taskDomain)
  }

  def testTaskEntity: CrawlerTaskEntity = {
    val task = new CrawlerTaskEntity
    task.jobId = 1
    task.jobName = "job.jobName.test.20150710"
    task.taskType = CrawlerTaskType.THEME_CRAWLER
    task.taskURI = "http://waimai.meituan.com/restaurant/177274"
    task.taskDomain = "waimai.meituan.com"
    task.cookies = "REMOVE_ACT_ALERT=1"
    task.isUseProxy = 0
    task.ruleUpdateType = CrawlerTaskRuleUpdateType.AUTO_INTELLIGENCE
    task.totalDepth = 1
    task.taskStartTime = System.currentTimeMillis()
    task.intervalTime = 10
    task.httpmethod = HttpRequestMethodType.GET
    task.topciCrawlerParserClassName = "com.foofv.crawler.parse.TestParser"
    task.currentDepth = 1
    task.totalBatch = 10
    task
  }
}

private[crawler] trait ParserManagerFactory {

  def createParserManager(): ParserManager
}
