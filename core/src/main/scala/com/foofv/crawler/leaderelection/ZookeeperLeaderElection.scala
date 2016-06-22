package com.foofv.crawler.leaderelection

import org.apache.curator.framework.recipes.leader.LeaderLatchListener
import com.foofv.crawler.util.Logging
import org.apache.curator.framework.CuratorFramework
import com.foofv.crawler.CrawlerConf
import org.apache.curator.framework.recipes.leader.LeaderLatch
import akka.actor.ActorRef
import com.foofv.crawler.zookeeper.ZKCuratorUtil
import com.foofv.crawler.parse.master.ParserMasterMsg._

/**
 * Parser need a master to monitor the worker,but we can't bear that the master breakdown.So this if for that
 * @author soledede
 */
private[crawler]
class ZookeeperLeaderElection(val masterActor: ActorRef,
    val masterUrl: String, val conf: CrawlerConf) extends LeaderElection with LeaderLatchListener with Logging {

  val WORKING_DIR = conf.get("crawler.zookeeper.dir", "/crawler") + "/leader_election"

  private var zk: CuratorFramework = _
  private var leaderLatch: LeaderLatch = _
  private var status = LeadershipStatus.NOT_LEADER

  override def preStart() {

    logInfo("Starting ZooKeeper LeaderElection")
    zk = ZKCuratorUtil.newClient(conf)
    leaderLatch = new LeaderLatch(zk, WORKING_DIR)
    leaderLatch.addListener(this)

    leaderLatch.start()
  }

  override def preRestart(reason: scala.Throwable, message: scala.Option[scala.Any]) {
    logError("LeaderElection failed...", reason)
    super.preRestart(reason, message)
  }

  override def postStop() {
    leaderLatch.close()
    zk.close()
  }

  override def receive = {
    case _ =>
  }

  override def isLeader() {
    synchronized {
      if (!leaderLatch.hasLeadership) {
        return
      }

      logInfo("We have gained leadership")
      updateLeadershipStatus(true)
    }
  }

  override def notLeader() {
    synchronized {
      if (leaderLatch.hasLeadership) {
        return
      }

      logInfo("We have lost leadership")
      updateLeadershipStatus(false)
    }
  }

  def updateLeadershipStatus(isLeader: Boolean) {
    if (isLeader && status == LeadershipStatus.NOT_LEADER) {
      status = LeadershipStatus.LEADER
      masterActor ! ElectedLeader
    } else if (!isLeader && status == LeadershipStatus.LEADER) {
      status = LeadershipStatus.NOT_LEADER
      masterActor ! RevokedLeadership
    }
  }
}