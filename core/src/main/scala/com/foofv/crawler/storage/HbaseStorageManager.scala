/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foofv.crawler.storage

import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import com.foofv.crawler.CrawlerConf
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.HBaseAdmin
import com.foofv.crawler.util.Logging
import org.apache.hadoop.hbase.HColumnDescriptor
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import org.apache.hadoop.hbase.util.Bytes
import com.foofv.crawler.hbase._
import com.foofv.crawler.entity.CrawlerTaskEntity
import scala.reflect.ClassTag

/**
 * @author soledede
 */
private[crawler] class HbaseStorageManager private (conf: CrawlerConf) extends StorageManager {
  val confHbase = HBaseConfiguration.create()
  confHbase.set("hbase.zookeeper.property.clientPort", conf.get("hbase.zookeeper.property.clientPort", "2181"))
  confHbase.set("hbase.zookeeper.quorum", conf.get("hbase.zookeeper.quorum", "spark1,spark2,spark4,spark5,spark7"))
  //z1,z2,z4,z5,z7
  //192.168.1.75,192.168.1.202,192.168.1.204,192.168.1.205,192.168.1.207
  //confHbase.set("hbase.master", conf.get("hbase.master", "192.168.1.75:60000"))
  //confHbase.addResource(conf.get("hbase.resource", "/opt/cloudera/parcels/CDH/lib/hbase/conf/hbase-site.xml"))
  confHbase.set(TableInputFormat.INPUT_TABLE, "crawler")

  val admin = new HBaseAdmin(confHbase)
  if (!admin.isTableAvailable("crawler")) {
    logInfo("Table Not Exists! Create crawler Table")
    val tableDesc = new HTableDescriptor("crawler")
    tableDesc.addFamily(new HColumnDescriptor("respinfo".getBytes()))
    admin.createTable(tableDesc)
  }
  val table = new HTable(confHbase, "crawler")

  override def getByKey[T: ClassTag](key: String): T = {
    this.synchronized{
    HbaseTool.getSingleValue("crawler", key, "respinfo", "resptaskid")
    }
  }

  override def listByKey[T: ClassTag](key: String): Seq[T] = throw new UnsupportedOperationException("unsupported")

  override def putByKey[T: ClassTag](key: String, entity: T): Boolean = {
    this.synchronized{
    HbaseTool.putSingleValue[T]("crawler", key, "respinfo", "resptaskid", entity)
    }
  }

  override def put[T: ClassTag](entity: T): Boolean = throw new UnsupportedOperationException("unsupported")

  override def delByKey[T: ClassTag](key: String): Boolean = {
    this.synchronized{
    HbaseTool.deleteRow("crawler",key)
    }
  }
}

object HbaseStorageManager {
  var hbaseStorageManager: HbaseStorageManager = null

  def main(args: Array[String]): Unit = {
    new HbaseStorageManager(new CrawlerConf).putByKey[CrawlerTaskEntity]("abcdefg", new CrawlerTaskEntity())
    println(new HbaseStorageManager(new CrawlerConf).getByKey[CrawlerTaskEntity]("abcdefg"))
  }

  def apply(conf: CrawlerConf): HbaseStorageManager = {
    if (hbaseStorageManager == null) hbaseStorageManager = new HbaseStorageManager(conf)
    hbaseStorageManager
  }

}