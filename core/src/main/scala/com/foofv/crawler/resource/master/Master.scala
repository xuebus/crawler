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
package com.foofv.crawler.resource.master

import scala.collection.mutable.HashSet
import scala.concurrent.duration._
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.parse.worker.WorkerInf
import com.foofv.crawler.supervise.Supervise
import com.foofv.crawler.util.ActorLogReceive
import com.foofv.crawler.util.Logging
import akka.actor.Actor
import akka.actor.ActorRef
import akka.remote.RemotingLifecycleEvent
import com.foofv.crawler.parse.master.ParserMasterMsg.CheckTimeOutForWorker
import com.foofv.crawler.supervise.NonSupervise
import akka.actor.Props
import akka.serialization.SerializationExtension
import com.foofv.crawler.supervise.ZookeeperSupervise
import com.foofv.crawler.leaderelection.SingleNodeLeader
import com.foofv.crawler.leaderelection.ZookeeperLeaderElection
import com.foofv.crawler.parse.master.ParserMasterMsg.ElectedLeader
import akka.actor.ActorSystem
import com.foofv.crawler.CrawlerException
import scala.concurrent.Await
import com.foofv.crawler.util.AkkaUtil
import com.foofv.crawler.util.Util
import scala.collection.mutable.HashMap
import com.foofv.crawler.deploy.TransferMsg._
import akka.actor.Address
import com.foofv.crawler.parse.worker.WorkerState
import akka.remote.DisassociatedEvent
import com.foofv.crawler.parse.master.RecoveryState
import com.foofv.crawler.rsmanager.Res
import java.util.concurrent.PriorityBlockingQueue
import java.util.Comparator
import java.util.PriorityQueue

/**
 * It need do what
 * manager memory,cpu of workers,
 * check the life of workers
 * @author soledede
 */
private[crawler] abstract class Master(
                                        val host: String,
                                        val port: Int,
                                        conf: CrawlerConf)
  extends Actor with ActorLogReceive with Logging {

  val workers = new HashSet[WorkerInf]
  val idToWorker = new HashMap[String, WorkerInf]
  val addressToWorker = new HashMap[Address, WorkerInf]

  val masterUrl = "crawler://" + host + ":" + port

  var supervise: Supervise = _ //data of landing

  var leaderElection: ActorRef = _

  val WORKER_TIMEOUT = conf.getLong("crawler.worker.timeout", 60) * 1000

  val REAPER_ITERATIONS = conf.getInt("crawler.dead.worker.persistence", 15)

  val RECOVERY_MODE = conf.get("crawler.recoveryMode", "NONE")

  var state = RecoveryState.STANDBY

  val priorityRes = new PriorityBlockingQueue[Res]() //sorting res by cpu mem

  val resAllocator = new ResourceAllocator(priorityRes)

  import context.dispatcher

  override def preStart() {

    context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])

    context.system.scheduler.schedule(0 millis, WORKER_TIMEOUT millis, self, CheckTimeOutForWorker)

    supervise = RECOVERY_MODE match {
      case "ZOOKEEPER" =>
        logInfo("Persisting recovery state to ZooKeeper")
        new ZookeeperSupervise(SerializationExtension(context.system), conf)
      case _ =>
        new NonSupervise()
    }

    leaderElection = RECOVERY_MODE match {
      case "ZOOKEEPER" =>
        context.actorOf(Props(classOf[ZookeeperLeaderElection], self, masterUrl, conf))
      case _ =>
        context.actorOf(Props(classOf[SingleNodeLeader], self))
    }

    initMaster
  }

  def initMaster = {}

  override def postStop() {
    context.stop(leaderElection)
  }

  override def receiveRecordLog = {
    case ElectedLeader => {
      val workInfs = supervise.redaPersistedObj()
      state = if (workInfs.isEmpty) {
        RecoveryState.ALIVE
      } else {
        RecoveryState.RECOVERING
      }
      logInfo("I have been elected leader! New state: " + state)
      if (state == RecoveryState.RECOVERING) {
        startRecovery(workInfs)
      }
    }
    case Task(task) => {
      println(task)
    }

    case RegisterWorker(id, workerHost, workerPort, cores, memory) => {
      logInfo(s"Registering Worker[$id] %s:%d with %d cores, %s RAM".format(
        workerHost, workerPort, cores, Util.megabytesToString(memory)))
      
      if (state == RecoveryState.STANDBY) {

      } else if (idToWorker.contains(id)) {
        sender ! RegisterWorkerFailed("Duplicate worker ID")
      } else {
        val worker = new WorkerInf(id, workerHost, workerPort, cores, memory, sender)
        if (registerWorker(worker)) {
          supervise.addWorker(worker)
          sender ! RegisteredWorker(masterUrl)
          println("Worker come in.........................")
        } else {
          val workerAddress = worker.actor.path.address
          logWarning("Worker registration failed. Attempted to re-register worker at same " +
            "address: " + workerAddress)
          sender ! RegisterWorkerFailed("Attempted to re-register worker at same address: "
            + workerAddress)
        }
      }
    }

    case Heartbeat(workerId, memUsage, cpuUsage, cpuCores, totalMem) => {
      idToWorker.get(workerId) match {
        case Some(workerInfo) =>
          workerInfo.lastHeartbeat = System.currentTimeMillis()
          //report resource cpu memory ..
          recordRes(workerId, memUsage, cpuUsage, cpuCores, totalMem)
          sender ! Active
        case None =>
          if (workers.map(_.id).contains(workerId)) {
            logWarning(s"Got heartbeat from unregistered worker $workerId." +
              " Asking it to re-register.")
            sender ! ReconnectWorker(masterUrl)
          } else {
            logWarning(s"Got heartbeat from unregistered worker $workerId." +
              " This worker was never registered, so ignoring the heartbeat.")
          }
      }
    }
    case CheckTimeOutForWorker => {
      timeOutDeadWorkers()
    }

    case DisassociatedEvent(_, address, _) => {
      logInfo(s"$address got disassociated, removing it.")
      addressToWorker.get(address).foreach(removeWorker)
    }


  }

  def timeOutDeadWorkers() {
    val currentTime = System.currentTimeMillis()
    val toRemove = workers.filter(_.lastHeartbeat < currentTime - WORKER_TIMEOUT).toArray
    for (worker <- toRemove) {
      if (worker.state != WorkerState.DEAD) {
        logWarning("Removing %s because we got no heartbeat in %d seconds".format(
          worker.id, WORKER_TIMEOUT / 1000))
        removeWorker(worker)
      } else {
        if (worker.lastHeartbeat < currentTime - ((REAPER_ITERATIONS + 1) * WORKER_TIMEOUT)) {
          workers -= worker
        }
      }
    }
  }

  def registerWorker(worker: WorkerInf): Boolean = {
    workers.filter { w =>
      (w.host == worker.host && w.port == worker.port) && (w.state == WorkerState.DEAD)
    }.foreach { w =>
      workers -= w
    }

    val workerAddress = worker.actor.path.address
    if (addressToWorker.contains(workerAddress)) {
      val oldWorker = addressToWorker(workerAddress)
      if (oldWorker.state == WorkerState.UNKNOWN) {
        removeWorker(oldWorker)
      } else {
        logInfo("Attempted to re-register worker at same address: " + workerAddress)
        return false
      }
    }

    workers += worker
    idToWorker(worker.id) = worker
    addressToWorker(workerAddress) = worker
    true
  }

  def removeWorker(worker: WorkerInf) {
    logInfo("Removing worker " + worker.id + " on " + worker.host + ":" + worker.port)
    worker.setState(WorkerState.DEAD)
    idToWorker -= worker.id
    addressToWorker -= worker.actor.path.address
    //res
    resAllocator.resHashMap -= worker.id
    resAllocator.refreshResList
    // println(resAllocator.resHashMap)
    //println(resAllocator.size)
    supervise.delWorker(worker)
  }

  def startRecovery(workInfs: Seq[WorkerInf]) = {
    for (worker <- workInfs) {
      logInfo("Trying to recover worker: " + worker.id)
      try {
        registerWorker(worker)
        worker.state = WorkerState.UNKNOWN
        worker.actor ! MasterChanged(masterUrl)
      } catch {
        case e: Exception => logInfo("Worker " + worker.id + " had exception on reconnect")
      }
    }
  }

  def recordRes(workerId: String, memUsage: Double, cpuUsage: Double, cpuCores: Int, totalMem: Double) = {
    //priorityRes.put()
    val res = Res(workerId, memUsage, cpuUsage, cpuCores, totalMem)
    resAllocator.resHashMap(res.workerId) = res.total
  }

  //get worker id acording available resource
  /*  def availableWorkerId(): String  = {
    var workerId: String = null
    if(priorityRes.size() > 0){
      val res = priorityRes.poll()
      workerId = res.workerId
      resHashMap(workerId)=res.total
    }else {
      if (resHashMap.size > 0) {
    	  workerId = resHashMap.toList.sortBy(_._2).reverse.head._1        
      }
    }
    workerId
  }*/
}

private[crawler] object Master extends Logging {

  val systemName = "crawlerMaster"
  private val actorName = "master"
  val crawlerUrlRegex = "crawler://([^:]+):([0-9]+)".r

  /* 
  def main(argStrings: Array[String]): Unit = {
    val conf = new CrawlerConf
     val args = new ParserMasterArguments(argStrings, conf)
    args.host = "192.168.10.25"
    args.port = 9999
    val (actorSystem, _) = startSystemAndActor(args.host, args.port, conf)
    actorSystem.awaitTermination()
  }*/

  /** Returns an `akka.tcp://...` URL for the Master actor given a crawlerUrl `crawler://host:ip`. */
  def toAkkaUrl(crawlerUrl: String): String = {
    crawlerUrl match {
      case crawlerUrlRegex(host, port) =>
        "akka.tcp://%s@%s:%s/user/%s".format(systemName, host, port, actorName)
      case _ =>
        throw new CrawlerException("Invalid Master URL: " + crawlerUrl)
    }
  }

  protected def startSystemAndActor(
                                     host: String,
                                     port: Int,
                                     conf: CrawlerConf): (ActorSystem, Int) = {
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    val actor = actorSystem.actorOf(Props(classOf[Master], host, boundPort), actorName)
    (actorSystem, boundPort)
  }
}