package com.foofv.crawler.leaderelection

import akka.actor.ActorRef
import akka.actor.Actor

/**
 * A LeaderElection keeps track of whether the current Master is the leader
 * @author soledede
 */
private[crawler]
trait LeaderElection extends Actor{
  val masterActor: ActorRef
}