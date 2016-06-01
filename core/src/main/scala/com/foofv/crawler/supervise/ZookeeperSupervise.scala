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
package com.foofv.crawler.supervise

import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Logging
import com.foofv.crawler.zookeeper.ZKCuratorUtil
import akka.serialization.Serialization
import scala.collection.JavaConversions._
import akka.actor.Actor
import akka.serialization.SerializationExtension
import com.foofv.crawler.parse.worker.WorkerInf

/**
 * persist state to zookeeper
 * @author soledede
 */
class ZookeeperSupervise(serialization: Serialization, conf: CrawlerConf) extends Supervise with Logging{
  
  
  
  val WORKING_DIR = conf.get("crawler.zookeeper.dir", "/crawler/parse") + "/master_status"
  val zk: CuratorFramework = ZKCuratorUtil.newClient(conf)

  ZKCuratorUtil.mkdir(zk, WORKING_DIR)



  private def serializeIntoFile(path: String, value: AnyRef) {
    val serializer = serialization.findSerializerFor(value)
    val serialized = serializer.toBinary(value)
    zk.create().withMode(CreateMode.PERSISTENT).forPath(path, serialized)
  }

  def deserializeFromFile[T](filename: String)(implicit m: Manifest[T]): Option[T] = {
    val fileData = zk.getData().forPath(WORKING_DIR + "/" + filename)
    val clazz = m.runtimeClass.asInstanceOf[Class[T]]
    val serializer = serialization.serializerFor(clazz)
    try {
      Some(serializer.fromBinary(fileData).asInstanceOf[T])
    } catch {
      case e: Exception => {
        logWarning("Exception while reading persisted file, deleting", e)
        zk.delete().forPath(WORKING_DIR + "/" + filename)
        None
      }
    }
  }
  
  override def addWorker(worker: WorkerInf){
    serializeIntoFile(WORKING_DIR + "/worker_" + worker.id, worker)
  }
  
  override def delWorker(worker: WorkerInf){
      zk.delete().forPath(WORKING_DIR + "/worker_" + worker.id)
  }
  
  override def redaPersistedObj(): Seq[WorkerInf] = {
    val persistsFiles = zk.getChildren().forPath(WORKING_DIR).toList.sorted
    val workerFiles = persistsFiles.filter(_.startsWith("worker_"))
    val workers = workerFiles.map(deserializeFromFile[WorkerInf]).flatten
     workers
  }
  
  override def close(){
    zk.close()
  }

 
}


/*object test extends Actor{
  def main(args: Array[String]): Unit = {
    val conf = new CrawlerConf
    new ZookeeperSupervise(SerializationExtension(context.system),conf)
  }
}*/