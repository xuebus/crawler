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
import com.aliyun.oss.OSSClient
import com.aliyun.oss.ClientConfiguration
import com.aliyun.oss.model.CannedAccessControlList
import com.foofv.crawler.serializer.Serializer
import java.io.DataInputStream
import org.apache.zookeeper.server.ByteBufferInputStream
import com.aliyun.oss.model.ObjectMetadata

/**
 * @author soledede
 */
private[crawler] class OSSStorageManager private (conf: CrawlerConf) extends StorageManager with Logging {

  private val accessKey = conf.get("storage.oss.v.accessKey", "BICHX0ajkgmPlIbn")
  private val accessKeySecret = conf.get("storage.oss.v.accessKeySecret", "bjz01zHzwpT2RghX6QwjaJty8xWdiW")
  private val endpoint = conf.get("storage.oss.v.endpoint", "http://oss-cn-shenzhen.aliyuncs.com/")
  private val bucketName = conf.get("storage.oss.bucketName", "crawler-oss-storagemager-master-worker")

  val ossConf = new ClientConfiguration()
  // 设置HTTP最大连接数为10
  ossConf.setMaxConnections(10);

  // 设置TCP连接超时为5000毫秒
  ossConf.setConnectionTimeout(10000);

  // 设置最大的重试次数为3
  ossConf.setMaxErrorRetry(0);

  // 设置Socket传输数据超时的时间为2000毫秒
  ossConf.setSocketTimeout(10000);

  // initOssClient
  val client = new OSSClient(endpoint, accessKey, accessKeySecret, ossConf)

  // 获取Bucket的存在信息
  val exists = client.doesBucketExist(bucketName)

  // 输出结果
  if (!exists) {
    // 新建一个Bucket
    client.createBucket(bucketName)
    //以设置为私有权限举例
    client.setBucketAcl(bucketName, CannedAccessControlList.Private);
    logError("Bucket " + bucketName + " created sucess!")
  } else logInfo("Bucket " + bucketName + " exists!")

  override def getByKey[T: ClassTag](key: String): T = {
    //this.synchronized {
    try {
      // 获取Object，返回结果为OSSObject对象
      val o = client.getObject(bucketName, key)
      // 获取Object的输入流
      val objectContent = o.getObjectContent()
      val entity = Serializer("java", new CrawlerConf).newInstance().deserialize(objectContent)
      // 关闭流
      objectContent.close()
      entity
    } catch {
      case e: Exception =>
        logError("get key[" + key + "] failed!", e)
        throw e
    }
    //}
  }

  override def getByKeyWithException[T: ClassTag](key: String): T = {
    //this.synchronized {
    try {
      // 获取Object，返回结果为OSSObject对象
      val o = client.getObject(bucketName, key)
      // 获取Object的输入流
      val objectContent = o.getObjectContent()
      val entity = Serializer("java", new CrawlerConf).newInstance().deserialize(objectContent)
      // 关闭流
      objectContent.close()
      entity
    } catch {
      case e: Exception =>
        throw e
    }
    //}
  }

  override def listByKey[T: ClassTag](key: String): Seq[T] = throw new UnsupportedOperationException("unsupported")

  override def putByKey[T: ClassTag](key: String, entity: T): Boolean = {
    //this.synchronized {
    try {
      val byteBuffer = Serializer("java", new CrawlerConf).newInstance().serialize(entity)
      // 创建上传Object的Metadata
      val metadata = new ObjectMetadata()
      // 必须设置ContentLength
      metadata.setContentLength(byteBuffer.capacity())
      val in = new ByteBufferInputStream(byteBuffer)
      val r = client.putObject(bucketName, key, in, metadata)
      logInfo(r.getETag)
      in.close()
      true
    } catch {
      case e: Exception =>
        logError("save [" + key + "] failed!", e)
        false
    }
    //}
  }

  override def put[T: ClassTag](entity: T): Boolean = throw new UnsupportedOperationException("unsupported")

  override def delByKey[T: ClassTag](key: String): Boolean = {
    //this.synchronized {
    try {
      // 删除Object
      client.deleteObject(bucketName, key)
      true
    } catch {
      case e: Exception =>
        logError("del " + key + "failed!")
        false
    }
    //}
  }
}

object OSSStorageManager {
  var ossStorageManager: OSSStorageManager = null

  def main(args: Array[String]): Unit = {
    /*    val oss =  new OSSStorageManager(new CrawlerConf)
    println(oss.putByKey[CrawlerTaskEntity]("abcdefg", new CrawlerTaskEntity()))
    println("删除前 "+oss.getByKey[CrawlerTaskEntity]("abcdefg"))
    println(oss.delByKey("abcdefg"))
    println("删除后"+oss.getByKey[CrawlerTaskEntity]("abcdefg"))*/

    testGetResObj
  }

  def apply(conf: CrawlerConf): OSSStorageManager = {
    if (ossStorageManager == null) ossStorageManager = new OSSStorageManager(conf)
    ossStorageManager
  }

  def testGetResObj() = {
    val oss = new OSSStorageManager(new CrawlerConf)
    try {
      val res = oss.getByKeyWithException("43b9ceb2-7fa8-4a10-8a6e-8ca56511b346#10_12345678_1")
      println(res)
    } catch {
      case e: java.net.SocketTimeoutException => {
        println(e.getMessage)
        e.printStackTrace()
      }
      case t: Exception => {
        //t.printStackTrace()
      }
    }
  }

}