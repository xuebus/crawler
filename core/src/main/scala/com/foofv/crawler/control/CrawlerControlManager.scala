package com.foofv.crawler.control

import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.parse.scheme.nodes.DocumentNode
import com.foofv.crawler.util.listener.{CrawlerTaskTraceListener, ManagerListenerWaiter}
import com.foofv.crawler.view.control.ControlWebView
import com.foofv.crawler.{CrawlerException, CrawlerConf}
import com.foofv.crawler.util.ActorLogReceive
import com.foofv.crawler.util.Logging
import com.foofv.crawler.deploy.TransferMsg._
import com.foofv.crawler.enumeration._
import com.foofv.crawler.parse.master.MasterArguments
import com.foofv.crawler.util.AkkaUtil

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.actor.ActorRef

private[crawler] class CrawlerControlManager(val host: String, val port: Int, conf: CrawlerConf) extends Actor
with ActorLogReceive with Logging {

  private var crawlerControl: CrawlerControl = null

  override def preStart(): Unit = {

    //listener waiter start
    val w = ManagerListenerWaiter()
    w.start()
    //post tast trace listerner
    val taskListener = new CrawlerTaskTraceListener(conf)
    w.addListener(taskListener)

    crawlerControl = CrawlerControl(conf)
    val web = new ControlWebView(8080, conf)
    web.bind()
  }

  override def postStop(): Unit = ()

  override def receiveRecordLog = {
    case Task(task) => {
      println(task)
    }
    case TaskJsonMsg(taskJsonMsg) => {
      val result = crawlerControl.addSeedTaskByJson(taskJsonMsg)
      sender ! "ok"
    }
    case TaskMsgMeituanTakeout(size, startIndex, batchSize, jobId, jobName) => {
      val result = crawlerControl.addTaskMeituanTakeoutMerchant(self, size, startIndex, batchSize, jobId, jobName)
      sender ! result
    }
    case content: String => {
      println(content)
    }
    case JobId(jobId: String) => {
      val t = CrawlerControlImpl.semanticTreeMap
      if (t != null && t.contains(jobId)) {
        val dTrees = t(jobId)
        if (dTrees != null) {
          sender ! DocTrees(jobId, dTrees)
        }
      }
    }
  }

}

private[crawler] object CrawlerControlManager {

  var actor: ActorRef = null
  val systemName = "crawlerControlManagerSys"
  private val actorName = "CrawlerControlManager"
  val controLUrlRegex = "control://([^:]+):([0-9]+)".r

  /** Returns an `akka.tcp://...` URL for the Master actor given a crawlerUrl `crawler://host:ip`. */
  def toAkkaUrl(controlUrl: String): String = {
    controlUrl match {
      case controLUrlRegex(host, port) =>
        "akka.tcp://%s@%s:%s/user/%s".format(systemName, host, port, actorName)
      case _ =>
        throw new CrawlerException("Invalid ControlMaster URL: " + controlUrl)
    }
  }

  def main(argString: Array[String]): Unit = {
    val conf = new CrawlerConf
    val args = new MasterArguments(argString, conf)
    //args.host = "192.168.10.146"
    //args.port = 9997
    //args.host = ""
    //args.port = 19997
    //args.host = "192.168.10.25"
    //args.port = 9997
    //control://192.168.10.25:9997


    val (actorSystem, _) = startSystemAndActor(args.host, args.port, conf)

    actorSystem.awaitTermination()
  }

  def startSystemAndActor(
                           host: String,
                           port: Int,
                           conf: CrawlerConf): (ActorSystem, Int) = {
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    actor = actorSystem.actorOf(Props(classOf[CrawlerControlManager], host, boundPort, conf), actorName)
    (actorSystem, boundPort)
  }
}