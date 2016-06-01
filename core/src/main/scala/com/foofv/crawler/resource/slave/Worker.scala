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
package com.foofv.crawler.resource.slave

import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import com.foofv.crawler.parse.scheme.nodes.{DocumentNode, SemanticNode}
import com.foofv.crawler.parse.topic.SchemeTopicParser
import com.foofv.crawler.util.listener.{CrawlerTaskTraceListener, ManagerListenerWaiter}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.CrawlerException
import com.foofv.crawler.deploy.TransferMsg._
import com.foofv.crawler.parse.worker.WorkerArguments
import com.foofv.crawler.resource.master.Master
import com.foofv.crawler.util.ActorLogReceive
import com.foofv.crawler.util.AkkaUtil
import com.foofv.crawler.util.Logging
import com.foofv.crawler.util.ResourseTool
import com.foofv.crawler.util.Util
import akka.actor.Actor
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.Cancellable
import akka.actor.Props
import akka.remote.RemotingLifecycleEvent
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.parse.master.ParserMasterMsg.CheckTimeOutForMaster

/**
 * Report cpu,memory,the number of idle thread and the number of work task to Master
 * @author soledede
 */
private[crawler] abstract class Worker(
                                        val host: String,
                                        val port: Int,
                                        val cores: Int,
                                        val memory: Int,
                                        val masterUrls: Array[String],
                                        val actorSystemName: String,
                                        val actorName: String,
                                        val conf: CrawlerConf,
                                        val controlUrl: String = "") extends Actor with ActorLogReceive with Logging {

  import context.dispatcher

  // Send a heartbeat every 10*1000 milliseconds
  val HEARTBEAT_MILLIS = conf.getLong("crawler.worker.hearbeat_milis", 5) * 1000

  val ACTIVE_TIMEOUT = conf.getLong("crawler.worker.timeout", 60) * 1000

  def createDateFormat = new SimpleDateFormat("yyyyMMddHHmmss")

  var master: ActorSelection = null
  var masterAddress: Address = null
  var activeMasterUrl: String = ""

  val akkaUrl = "akka.tcp://%s@%s:%s/user/%s".format(actorSystemName, host, port, actorName)

  val workerId = generateWorkerId()
  
  Worker._workerId = workerId

  var coresUsed = 0
  var memoryUsed = 0
  var connectionAttemptCount = 0

  @volatile var connected = false

  def coresFree: Int = cores - coresUsed

  def memoryFree: Int = memory - memoryUsed

  var lastHeartbeat = 0L

  var registrationRetryTimer: Option[Cancellable] = None

  val INITIAL_REGISTRATION_RETRIES = 6
  val TOTAL_REGISTRATION_RETRIES = INITIAL_REGISTRATION_RETRIES + 10
  val FUZZ_MULTIPLIER_INTERVAL_LOWER_BOUND = 0.500

  val REGISTRATION_RETRY_FUZZ_MULTIPLIER = {
    val randomNumberGenerator = new Random(UUID.randomUUID.getMostSignificantBits)
    randomNumberGenerator.nextDouble + FUZZ_MULTIPLIER_INTERVAL_LOWER_BOUND
  }
  val INITIAL_REGISTRATION_RETRY_INTERVAL = (math.round(10 *
    REGISTRATION_RETRY_FUZZ_MULTIPLIER)).seconds
  val PROLONGED_REGISTRATION_RETRY_INTERVAL = (math.round(60
    * REGISTRATION_RETRY_FUZZ_MULTIPLIER)).seconds

  @volatile var registered = false

  override def preStart() {
    logInfo("Starting Crawler worker %s:%d with %d cores, %s RAM".format(
      host, port, cores, Util.megabytesToString(memory)))
    context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])
    registerAllMasters

    val w = ManagerListenerWaiter()
    w.start()
    //post tast trace listerner
    val taskListener = new CrawlerTaskTraceListener(conf)
    w.addListener(taskListener)

    intiWorker

    context.system.scheduler.schedule(0 millis, ACTIVE_TIMEOUT millis, self, CheckTimeOutForMaster)
  }

  protected def intiWorker = {}

  override def receiveRecordLog = {
    case RegisteredWorker(masterUrl) =>
      logInfo("Successfully registered with master " + masterUrl)
      registered = true
      connected = true
      changeMaster(masterUrl)
      context.system.scheduler.schedule(0 millis, HEARTBEAT_MILLIS millis, self, SendHeartbeat)

    case SendHeartbeat =>
      if (connected) {
        master ! Heartbeat(workerId, ResourseTool.getMemInfo.memUsage + activeThreadCount * 1024 * 60, ResourseTool.getCpuInfo.cpuUsage, ResourseTool.getCpuInfo.cpuCores, ResourseTool.getMemInfo.memTotal)
      }


    case MasterChanged(masterUrl) =>
      logInfo("Master has changed, new master is at " + masterUrl)
      changeMaster(masterUrl)

    case RegisterWorkerFailed(message) =>
      if (!registered) {
        logError("Worker registration failed: " + message)
        System.exit(1)
      }

    case ReconnectWorker(masterUrl) =>
      logInfo(s"Master with url $masterUrl requested this worker to reconnect.")
      registerWithMaster()

    case ReregisterWithMaster =>
      reregisterWithMaster()

    // AgentWorker 
    case HandleTaskEntity(task) =>
      handleTaskEntity(task)

    case Active =>
      lastHeartbeat = System.currentTimeMillis()

    case CheckTimeOutForMaster => {
      timeOutDeadMaster()
      //SchemeTopicParser.controlActor tell(JobId("1234567890"), self)
    }

    // parser queue message 
    case TaskKeyMsg(msg: String) =>
      logDebug("Worker have gotten identity key from storage!")

      handleMsg(msg)

    case TestDocTrees(jobId: String, tree: mutable.Map[String, SemanticNode]) => {
      handleScheme(jobId, tree)
    }


    /* case JobId(jobId: String) =>
       master ! jobId*/
    /* case TestMsg(msg: String) =>
       println(msg)*/
  }

  private def timeOutDeadMaster() = {
    val currentTime = System.currentTimeMillis()
    if (lastHeartbeat <= currentTime - ACTIVE_TIMEOUT) {
      reregisterWithMaster()
    }
  }

  protected def activeThreadCount: Int = {
    0
  }

  protected def handleScheme(jobId: String, tree: mutable.Map[String, SemanticNode]) = {}

  protected def handleMsg(msg: String) = {}

  protected def handleTaskEntity(taskEntity: CrawlerTaskEntity) {}

  protected def changeMaster(url: String)
  
  def generateWorkerId(): String = {
    //"worker--%s-%d".format(createDateFormat.format(new Date), host, port)
    "worker-%s-%d".format(host, port)
  }

  protected def registerAllMasters()

  private def reregisterWithMaster(): Unit = {
    Util.tryOrExit {
      connectionAttemptCount += 1
      if (registered) {
        registrationRetryTimer.foreach(_.cancel())
        registrationRetryTimer = None
      } else if (connectionAttemptCount <= TOTAL_REGISTRATION_RETRIES) {
        logInfo(s"Retrying connection to master (attempt # $connectionAttemptCount)")
        if (master != null) {
          master ! RegisterWorker(
            workerId, host, port, cores, memory)
        } else {
          registerAllMasters()
        }
        // We have exceeded the initial registration retry threshold
        // All retries from now on should use a higher interval
        if (connectionAttemptCount == INITIAL_REGISTRATION_RETRIES) {
          registrationRetryTimer.foreach(_.cancel())
          registrationRetryTimer = Some {
            context.system.scheduler.schedule(PROLONGED_REGISTRATION_RETRY_INTERVAL,
              PROLONGED_REGISTRATION_RETRY_INTERVAL, self, ReregisterWithMaster)
          }
        }
      } else {
        logError("All masters are unresponsive! Giving up.")
        System.exit(1)
      }
    }
  }

  def registerWithMaster() {
    registrationRetryTimer match {
      case None =>
        registered = false
        registerAllMasters()
        connectionAttemptCount = 0
        registrationRetryTimer = Some {
          context.system.scheduler.schedule(INITIAL_REGISTRATION_RETRY_INTERVAL,
            INITIAL_REGISTRATION_RETRY_INTERVAL, self, ReregisterWithMaster)
        }
      case Some(_) =>
        logInfo("Not spawning another attempt to register with the master, since there is an" +
          " attempt scheduled already.")
    }
  }

}

private[crawler] object Worker {

  var WorkerIp = ""
  var _workerId = ""
}

/*private[crawler] object Worker extends Logging {
  def main(argStrings: Array[String]): Unit = {

    val conf = new CrawlerConf
    val args = new WorkerArguments(argStrings, conf)
    args.masters(0) = "crawler://192.168.10.25:9999"
    val (actorSystem, _) = startSystemAndActor(args.host, args.port, args.cores, args.memory, args.masters)
    actorSystem.awaitTermination()
   

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
    val actorName = "Worker"
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    actorSystem.actorOf(Props(classOf[Worker], host, boundPort, cores, memory,
      masterUrls, systemName, actorName, conf), name = actorName)
    (actorSystem, boundPort)
  }

}*/