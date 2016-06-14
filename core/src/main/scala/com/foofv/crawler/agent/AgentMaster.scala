/**
 *
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
package com.foofv.crawler.agent

import java.util.concurrent.TimeUnit
import com.foofv.crawler.parse.worker.WorkerInf
import scala.collection.mutable.HashMap
import scala.util.control.Breaks._
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.CrawlerException
import com.foofv.crawler.antispamming._
import com.foofv.crawler.blockqueue.CrawlerBlockQueue
import com.foofv.crawler.blockqueue.ICrawlerBlockQueueFactory
import com.foofv.crawler.deploy.TransferMsg._
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.parse.master.MasterArguments
import com.foofv.crawler.resource.master.Master
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.schedule.ITaskQueueFactory
import com.foofv.crawler.util.AkkaUtil
import com.google.common.cache._
import akka.actor.ActorSystem
import akka.actor.Props
import com.foofv.crawler.util.constant.Constant
import scala.collection.mutable.ArrayBuffer
import com.foofv.crawler.enumeration.CrawlerDTWorkerType
import com.foofv.crawler.redis.cli.RedisOps
import com.foofv.crawler.util._

/**
 * Schedule resource 、 tast 。。。
 */
private[crawler] class AgentMaster(
  override val host: String,
  override val port: Int,
  conf: CrawlerConf)
  extends Master(host, port, conf) with ICrawlerBlockQueueFactory with IAntiSpammingFactory {

  //open two threads move task from redis queue to local block queue

  //loop get task from queue,like redis 

  //Schedule resource 

  //the strategy of anti spamming

  //serializer  crawerentity and send it to worker

  var allocatorCnt = 0

  var crawlerDTScheduleTask: CrawlerDTWorker = null
  var crawlerDTScheduleTaskMonitor: CrawlerDTWorker = null

  val localTaskBlockQueue = getCrawlerBlockQueue[CrawlerTaskEntity]

  val taskSortedQueue = ITaskQueue("sortSet", conf)
  val taskSortedQueueName = Constant(conf).CRAWLER_TASK_SORTEDSET_KEY

  override def initMaster() {
    val interval = Constant(conf).CRAWLER_DTWORKER_DEFAULT_WORK_INTERVAL
    //get CrawlerTaskEntity from Redis.SortedSet
    //put CrawlerTaskEntity to  Redis.list
    val crawlerDTPutTaskEntityToRedisList = new CrawlerDTWorker(name = "PutTaskEntityToRedisList", interval = interval, callback = putTaskEntityToRedisList _)
    crawlerDTPutTaskEntityToRedisList.startUp()

    //get CrawlerTaskEntity from Redis.list
    //put CrawlerTaskEntity to CrawlerBlockQueue
    val crawlerDTPutTaskEntityToLocalBlockQueue = new CrawlerDTWorker(name = "PutTaskEntityToLocalBlockQueue", interval = interval, callback = putTaskEntityToLocalBlockQueue _)
    crawlerDTPutTaskEntityToLocalBlockQueue.startUp()

    //take CrawlerTaskEntity from CrawlerBlockQueue
    //get an AgentWorker and send CrawlerTaskEntity to AgentWorker
    crawlerDTScheduleTask = new CrawlerDTWorker(name = "ScheduleTask", interval = interval, callback = scheduleTask _, isRestartIfTimeout = checkLocalTaskBlockQueue _)
    crawlerDTScheduleTask.startUp()

    //monitor CrawlerDTScheduleTask.workerDaemonThread state
    crawlerDTScheduleTaskMonitor = new CrawlerDTWorker(name = "MonitorScheduleTask", interval = interval, callback = minotorScheduleTask _)
    crawlerDTScheduleTaskMonitor.startUp()

    logInfo("AgentMaster start up")

  }

  private def minotorScheduleTask = {
    val now = System.currentTimeMillis()
    val workerDaemonName = crawlerDTScheduleTask.workerDaemonName
    logDebug("check " + workerDaemonName + " running state")
    val lastTimeCache = Option(crawlerDTScheduleTask.cacheManager.getIfPresent(workerDaemonName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerDaemonName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        logInfo(workerDaemonName + " may be dead")
        crawlerDTScheduleTask.workerDaemonThread.stop(true)
        crawlerDTScheduleTask.workerDaemonThread.restart()
        crawlerDTScheduleTaskMonitor.updateCache(workerDaemonName, now)
      }
    }
    -1
  }

  private val redisTaskList = ITaskQueue("list", conf)
  private val redisTaskListKey = Constant(conf).CRAWLER_TASK_LIST_KEY
  private def putTaskEntityToLocalBlockQueue = {
    var newPeriod = -1L
    logDebug("putTaskEntityToLocalBlockQueue START")
    val task = redisTaskList.getLastElement[CrawlerTaskEntity](redisTaskListKey)
    if (task == null) {
      logDebug("Redis.List is empty")
      //val offsetTime = this.getDefaultIntervaltime * (util.Random.nextInt(CrawlerDTPutTaskToBlockQueue.ScanIntervalTimes)+1)
      //logDebug("offsetTime "+offsetTime)
      //newPeriod = offsetTime
    } else {
      localTaskBlockQueue.put(task)
      logDebug("put task[" + task.taskId + "] to CrawlerBlockQueue")
      logInfo("LocalBlockQueue size[" + localTaskBlockQueue.size() + "]")
      newPeriod = 1
    }
    logDebug("putTaskEntityToLocalBlockQueue END")
    newPeriod
  }

  private val redisTaskSortedSet = ITaskQueue("sortSet", conf)
  private val redisTaskSortedSetKey = Constant(conf).CRAWLER_TASK_SORTEDSET_KEY
  private val redisClient: RedisOps = RedisOps.createRedis("akka", conf)
  private val fetchNextTaskIntervalKey = Constant(conf).CRAWLER_GET_NEXT_TASK_INTERVAL_KEY
  private var ifExistAgentWorker = (true, System.currentTimeMillis())
  private def putTaskEntityToRedisList = {
    var newPeriod = -1L
    logDebug("putTaskEntityToRedisList START")
    val currentTime = System.currentTimeMillis()
    if (currentTime - ifExistAgentWorker._2 > 60 * 1000) {
      ifExistAgentWorker = (true, System.currentTimeMillis())
    }
    val existAgentWorker = ifExistAgentWorker._1
    if (existAgentWorker) {
      val result = Option[(CrawlerTaskEntity, Double)](redisTaskSortedSet.getFirstElementWithScore[CrawlerTaskEntity](redisTaskSortedSetKey))
      val task = result.get._1
      if (task == null) {
        logInfo("Redis.SortedSet is empty")
      } else {
        val score = result.get._2
        if (currentTime >= score) {
          //TODO tx
          redisTaskList.putElementToList(redisTaskListKey, task)
          logDebug("add task [" + task.taskId + "] to Redis.List")
          redisTaskSortedSet.delFirstElement(redisTaskSortedSetKey)
          logDebug("remove task [" + task.taskId + "] from Redis.SortedSet")
          newPeriod = getFetchGetNextTaskInterval(fetchNextTaskIntervalKey)
        } else {
          logInfo("score " + score.toLong + ",retry it later...")
          //val taskId = task.taskId
          //newPeriod = (score - currentTime).toLong + 200
          //logDebug(s"task[$taskId] should be execute in [$newPeriod]milliseconds, just wait...")
        }
      }
    } else {
      logInfo("workers may be not present now")
    }
    logDebug("putTaskEntityToRedisList END")
    newPeriod
  }

  private val fetchNextTaskIntervalCacheLoader: CacheLoader[java.lang.String, java.lang.Long] =
    new CacheLoader[java.lang.String, java.lang.Long]() {
      def load(key: java.lang.String): java.lang.Long = {
        1000L
      }
    }
  val fetchNextTaskIntervalCacheManager = CacheBuilder.newBuilder()
    .expireAfterWrite(30 * 1000, TimeUnit.MILLISECONDS).build(fetchNextTaskIntervalCacheLoader)

  private def getFetchGetNextTaskInterval(key: String): Long = {
    var interval = fetchNextTaskIntervalCacheManager.getIfPresent(key)
    if (interval == null) {
      interval = redisClient.getValue[String](key).getOrElse("500").toLong
      fetchNextTaskIntervalCacheManager.put(key, interval)
      logInfo(s"fetchNextTaskInterval[$interval]")
    }
    interval
  }

  //check CrawlerBlockQueue state
  //if size equals 0, CrawlerBlockQueue may be empty for a time
  //so CrawlerDTScheduleTask.workerDaemonThread need not to check CrawlerDTScheduleTask.workerThread state
  //if not, should do it.
  private def checkLocalTaskBlockQueue(): Boolean = {
    if (localTaskBlockQueue.size() == 0 ) {
      logInfo("localBlockQueue size[" + localTaskBlockQueue.size() + "], DO NOT restart ScheduleTask worker-thread")
      false
    } else if(ifExistAgentWorker._1==false) {
      logInfo("workers are not present, DO NOT restart ScheduleTask worker-thread")
      false
    }else {
      true
    }
  }

  // (domain, timestamp)
  private val scheduleTaskDelayTime = Constant(conf).CRAWLER_AGENT_MASTER_SCHEDULE_TASK_DELAY_TIME_IF_TOO_FAST
  private val scheduleTaskRetryIntervalIfNoWorks = Constant(conf).CRAWLER_AGENT_MASTER_SCHEDULE_TASK_RETRY_INTERVAL_IF_NO_WORKERS

  //schedule CrawlerTaskEntity, callback of CrawlerDTScheduleTask
  //if get an AgentWorker, send CrawlerTaskEntity to it
  //if not send CrawlerTaskEntity back to Redis.SortedSet
  private def scheduleTask() = {
    logInfo("schedule task START")
    while (true) {
      val taskEntity: CrawlerTaskEntity = localTaskBlockQueue.take()
      crawlerDTScheduleTask.updateWorderThreadCache()
      val taskDomain = taskEntity.taskDomain
      val taskId = taskEntity.taskId
      CrawlerDTScheduleTask.taskCounter += 1
      logInfo(s"fetch TaskEntity[$taskId]")
      logInfo("fetch TaskEntity count: " + CrawlerDTScheduleTask.taskCounter)
      resAllocator.refreshResList
      var cnt = 0
      breakable {
        while (true) {
          val availableWorkerSize = resAllocator.size
          if (availableWorkerSize <= 0) {
            logInfo(s"No AgentWorker found for TaskEntity[$taskId], put back to SortedSet, retry 300 seconds later")
            ifExistAgentWorker = (false, System.currentTimeMillis())
            taskSortedQueue.putElementToSortedSet(taskSortedQueueName, taskEntity, System.currentTimeMillis() + 3 * 100 * 1000)
            break
          } else {
            val workerId = resAllocator.availableWorkerId(this)
            if (workerId == null) {
              logInfo(s"no available AgentWorker")
              taskSortedQueue.putElementToSortedSet(taskSortedQueueName, taskEntity, System.currentTimeMillis() + 1 * 10 * 1000)
              logInfo(s"put TaskEntity[$taskId] to TaskSortedSet and retry it 10 seconds later")
              break
            } else {
              //allocate task key
              if (idToWorker.contains(workerId)) {
                cnt = 0
                val workerInf = idToWorker(workerId)
                if (!checkAntiSpamming(workerInf.id, taskEntity.taskDomain)) {
                  allocatorRes(taskEntity, workerInf)
                  break
                } else {
                  val avalibleRes = resAllocator.resList.filter(x =>
                    !checkAntiSpamming(idToWorker(x._1).id, taskEntity.taskDomain))
                  if (avalibleRes != null && avalibleRes.size > 0) {
                    allocatorRes(taskEntity, idToWorker(avalibleRes(0)._1))
                    break
                  }
                  taskSortedQueue.putElementToSortedSet(taskSortedQueueName, taskEntity, System.currentTimeMillis() + scheduleTaskRetryIntervalIfNoWorks)
                  logInfo(s"put TaskEntity [$taskId] to TaskSortedSet and retry it later,the interval [$scheduleTaskRetryIntervalIfNoWorks] get from redes!")
                  break
                  // }
                  //}
                }
                //}
              } else {
                Thread.sleep(10 * 1000)
                cnt += 1
                if (cnt >= 5) logWarning(s"I have retry find parserworker [$cnt] times, all parser worker may be dead!")
              }
            }
          }
        }
      }
      //}
      //allocatorCnt = 0
      //}
    }
    -1
  }

  def allocatorRes(taskEntity: CrawlerTaskEntity, workerInf: WorkerInf): Unit = {
    logInfo("find AgentWorker [" + workerInf.host + "] for TaskEntity [" + taskEntity.taskId + "]")
    workerInf.actor ! HandleTaskEntity(taskEntity)
    allocatorCnt = 0
  }
  
  val cacheLoader: CacheLoader[java.lang.String, java.lang.Boolean] =
    new CacheLoader[java.lang.String, java.lang.Boolean]() {
      def load(key: java.lang.String): java.lang.Boolean = {
        false
      }
    }
  val antiSpammingCacheManager = CacheBuilder.newBuilder()
    .expireAfterWrite(60 * 2, TimeUnit.SECONDS).build(cacheLoader)
    
  val antiSpam: AntiSpamming = createAntiSpamming

  private def checkAntiSpamming(workerId: String, domain: String): Boolean = {
    val key = workerId.trim() + domain.trim()
    var value = antiSpammingCacheManager.getIfPresent(key)
    if (value == null) {
      value = antiSpam.isRefused(workerId.trim(), domain.trim())
      antiSpammingCacheManager.put(key, value)
    }
    logDebug(s"checkAntiSpamming Key[$key], Value[$value]")
    value
  }

  def getCrawlerBlockQueue[T]: CrawlerBlockQueue[T] = {
    CrawlerBlockQueue[T]("linkedBlock", "getTask", conf)
  }

  def createAntiSpamming: AntiSpamming = {
    new RedisAntiSpamming(conf)
  }

}

private[crawler] object AgentMaster {

  val systemName = "crawlerMasterSys"
  private val actorName = "AgentMaster"
  val crawlerUrlRegex = "crawler://([^:]+):([0-9]+)".r

  def main(argStrings: Array[String]): Unit = {
    val conf = new CrawlerConf
    val args = new MasterArguments(argStrings, conf)
    //args.host = "192.168.10.146"
    //     args.port = 9999
    //    args.host = "192.168.10.47"
    //args.port = 9999
    //args.host = "192.168.10.25"
    //args.port = 9999

    val (actorSystem, _) = startSystemAndActor(args.host, args.port, conf)
    actorSystem.awaitTermination()
  }

  /** Returns an `akka.tcp://...` URL for the Master actor given a crawlerUrl `crawler://host:ip`. */
  def toAkkaUrl(crawlerUrl: String): String = {
    crawlerUrl match {
      case crawlerUrlRegex(host, port) =>
        "akka.tcp://%s@%s:%s/user/%s".format(systemName, host, port, actorName)
      case _ =>
        throw new CrawlerException("Invalid AgentMaster URL: " + crawlerUrl)
    }
  }

  def startSystemAndActor(
    host: String,
    port: Int,
    conf: CrawlerConf): (ActorSystem, Int) = {
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    val actor = actorSystem.actorOf(Props(classOf[AgentMaster], host, boundPort, conf), actorName)
    (actorSystem, boundPort)
  }

  //proxyip_domain->(proxyStartTime，currentProxyIndex,haveUserdProxyNum)
  /*
  val proxytMap = new HashMap[String, (Long, Int, Int)]()
  var httpProxyArray: Array[(String, Int)] = Array(
    ("183.250.91.33", 8080),
    ("218.56.132.155", 8080),
    ("122.243.10.223", 9000) ,
    ("218.203.105.31", 80),
    ("221.182.62.115", 9999),
    ("218.92.227.166", 33948),
    ("117.136.234.4", 80),
    ("218.92.227.166", 18256),
    ("218.92.227.169", 33919),
    ("117.177.243.50", 82),
    ("111.79.177.120", 9000),
    ("39.190.168.100", 8123),
    ("218.92.227.166", 20771),
    ("183.62.60.100", 80),
    ("183.249.28.212", 8123),
    ("58.247.69.243", 8090),
    ("112.1.165.177", 8123),
    ("120.195.195.212", 80),
    ("59.108.201.236", 80),
    ("221.176.14.72", 80),
    ("218.92.227.170", 16107),
    ("120.195.192.131", 80),
    ("14.152.49.194", 80),
    ("220.198.91.61", 9999),
    ("218.92.227.171", 16107),
    ("120.195.194.105", 80),
    ("183.249.29.247", 8123),
    ("117.136.234.18", 80),
    ("219.142.192.196", 33319),
    ("114.42.126.88", 80),
    ("111.40.196.70", 84),
    ("218.92.227.173", 17403),
    ("49.94.151.205", 3128),
    ("218.92.227.168", 17945),
    ("120.195.200.41", 80),
    ("112.84.130.14", 80),
    ("183.221.147.53", 8123),
    ("59.108.201.241", 80),
    ("218.92.227.165", 19329),
    ("218.92.227.170", 20771),
    ("58.220.2.140", 80),
    ("117.177.243.30", 81),
    ("49.91.6.141", 80),
    ("120.197.234.164", 80),
    ("163.125.66.3", 9999),
    ("117.177.243.51", 83),
    ("202.100.167.175", 80),
    ("114.255.183.163", 8080),
    ("123.245.130.78", 8090),
    ("218.92.227.171", 11095),
    ("218.92.227.170", 19279),
    ("218.92.227.171", 35010),
    ("117.177.243.36", 86),
    ("219.142.192.196", 2920),
    ("101.71.27.120", 80),
    ("111.12.117.68", 8082),
    ("218.92.227.168", 19279),
    ("183.147.23.86", 9000),
    ("183.230.53.136", 8123),
    ("49.90.2.38", 3128),
    ("218.92.227.168", 13789),
    ("183.227.208.117", 8123),
    ("39.170.253.121", 8123),
    ("39.182.165.78", 8123),
    ("121.15.230.126", 9797),
    ("121.34.128.112", 9999),
    ("120.195.196.159", 80),
    ("49.94.7.201", 3128),
    ("121.69.15.58", 8118),
    ("59.108.201.237", 80),
    ("114.93.34.56", 9000),
    ("218.92.227.171", 18204),
    ("1.193.201.177", 9999),
    ("49.94.12.93", 80),
    ("218.92.227.173", 33944),
    ("218.92.227.165", 13374),
    ("117.177.243.30", 8080),
    ("117.177.243.37", 81),
    ("218.92.227.168", 34034),
    ("106.33.69.159", 80),
    ("202.100.167.173", 80),
    ("218.95.82.229", 9000),
    ("223.68.3.171", 80),
    ("110.176.187.145", 3128),
    ("218.92.227.170", 34043),
    ("121.28.210.164", 9000),
    ("117.177.243.102", 83),
    ("111.161.126.101", 80),
    ("223.93.88.156", 8123),
    ("117.177.243.42", 8081),
    ("223.68.3.190", 80),
    ("222.188.102.156", 8086),
    ("219.142.192.196", 648),
    ("223.100.98.44", 8000),
    ("39.190.143.179", 8123),
    ("219.142.192.196", 1834),
    ("117.136.234.10", 80),
    ("59.127.154.78", 80),
    ("117.177.243.26", 84),
    ("218.92.227.172", 23685),
    ("120.195.206.51", 80),
    ("223.68.3.147", 80) )
    */
}

