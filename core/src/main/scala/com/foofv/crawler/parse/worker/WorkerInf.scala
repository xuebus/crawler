package com.foofv.crawler.parse.worker

import akka.actor.ActorRef

/**
 * Entity for parse Worker
 * @author soledede
 */
private[crawler] class WorkerInf(
  val id: String,
  val host: String,
  val port: Int,
  val cores: Int,
  val memory: Int,
  val actor: ActorRef) extends Serializable {

  @transient var coresUsed: Int = _
  @transient var memoryUsed: Int = _
  @transient var state: WorkerState.Value = _

  @transient var lastHeartbeat: Long = _

  init()

  def coresFree: Int = cores - coresUsed
  def memoryFree: Int = memory - memoryUsed

  private def init() {
    state = WorkerState.ALIVE
    coresUsed = 0
    memoryUsed = 0
    lastHeartbeat = System.currentTimeMillis()
  }

  def setState(state: WorkerState.Value) = {
    this.state = state
  }

}