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
package com.foofv.crawler.deploy

import com.foofv.crawler.parse.scheme.nodes.{DocumentNode, SemanticNode}
import com.foofv.crawler.util.Util
import com.foofv.crawler.entity.CrawlerTaskEntity
import akka.actor.ActorRef

import scala.collection.mutable

/**
 * @author soledede
 */
private[crawler] sealed
trait TransferMsg extends Serializable

private[crawler] object TransferMsg{
  
    // Master to Worker
  case class MasterChanged(masterUrl: String) extends TransferMsg
  
  // ParserWorker to ParserMaster

  case class RegisterWorker(
      id: String,
      host: String,
      port: Int,
      cores: Int,
      memory: Int)
    extends TransferMsg {
    Util.checkHost(host, "Required hostname")
    assert (port > 0)
  }
  
  case class RegisterWorkerFailed(message: String) extends TransferMsg
  
  //Finished register from worker to master
  case class RegisteredWorker(masterUrl: String) extends TransferMsg
  
  //tell the worker ,reconnect master please
  case class ReconnectWorker(masterUrl: String) extends TransferMsg
  
  case object SendHeartbeat  extends TransferMsg
  
  case object Active extends TransferMsg
  
  case class Heartbeat(workerId: String,memUsage: Double,cpuUsage: Double,cpuCores: Int,totalMem: Double)  extends TransferMsg
  
  
  case object ReregisterWithMaster 
  
  case class Task(entity: CrawlerTaskEntity) extends TransferMsg
  
  case class HandleTaskEntity(entity: CrawlerTaskEntity) extends TransferMsg

  case class FetchSchemeTree(jobId: String) extends TransferMsg
  
  case object Father
  
  case class TaskKeyMsg(msg: String) extends TransferMsg
  
  case class TaskJsonMsg(msg: String) extends TransferMsg
  
  case class TaskMsgMeituanTakeout(size: Int = 500000, startIndex: Int = 1, batchSize: Int = 500, jobId: Long = System.currentTimeMillis(), jobName: String = "job_MeituanTakeoutMerchant") extends TransferMsg

  case class TestMsg(msg: String)

  case class JobId(jobId: String) extends TransferMsg

  case class DocTrees(jobId:String,tree: mutable.Map[String, SemanticNode]) extends TransferMsg

  case class TestDocTrees(jobId:String,tree: mutable.Map[String, SemanticNode]) extends TransferMsg

  case class TestNode(node:DocumentNode) extends TransferMsg

  case class TestTaskEntity(task: CrawlerTaskEntity) extends  TransferMsg
}

