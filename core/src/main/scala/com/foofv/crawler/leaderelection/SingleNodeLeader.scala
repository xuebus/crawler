package com.foofv.crawler.leaderelection

import akka.actor.ActorRef
import com.foofv.crawler.parse.master.ParserMasterMsg.ElectedLeader

/**
 * Single-node
 * @author soledede
 */
class SingleNodeLeader(val masterActor: ActorRef) extends LeaderElection {
  override def preStart() {
    masterActor ! ElectedLeader
  }

  override def receive = {
    case _ =>
  }
}