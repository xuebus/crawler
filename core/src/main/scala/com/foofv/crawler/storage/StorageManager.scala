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
package com.foofv.crawler.storage

import com.foofv.crawler.util.Logging
import com.foofv.crawler.CrawlerConf

import scala.reflect.ClassTag
import org.apache.http.HttpResponse
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.enumeration.HttpRequestMethodType._
import org.apache.commons.lang3.NotImplementedException

import scala.collection.mutable.Map

/**
  * storage interface
  *
  * @author soledede
  */
private[crawler] trait StorageManager extends Logging {

  def getByKey[T: ClassTag](key: String): T = {
    null.asInstanceOf[T]
  }

  def getByKeyWithException[T: ClassTag](key: String): T = {
    throw new Exception("Not Implemented")
  }

  def asynByKey(key: String, f: (HttpResponse) => Unit) = {}

  def listByKey[T: ClassTag](key: String): Seq[T] = {
    null.asInstanceOf[Seq[T]]
  }

  def putByKey[T: ClassTag](key: String, entity: T): Boolean = {
    false
  }

  def put[T: ClassTag](entity: T): Boolean = {
    false
  }

  def put(tableName: String, entitys: Seq[Map[String, AnyRef]]): Boolean = false

  def delByKey[T: ClassTag](key: String): Boolean = {
    false
  }

}

private[crawler] trait StorageManagerFactory {
  def createStorageManager(): StorageManager
}

object StorageManager {
  def apply(storageType: String, conf: CrawlerConf): StorageManager = {
    storageType match {
      case "hbase" => HbaseStorageManager(conf)
      case "mongo" => MongoStorageManager(conf)
      case "oss" => OSSStorageManager(conf)
      case "local" => LocalStorageManager(conf)
      case _ => null
    }
  }
}



object TestHbaseStorageManager {
  def main(args: Array[String]): Unit = {
    test
  }

  def test = {
    val conf: CrawlerConf = new CrawlerConf
    val hbaseSM = StorageManager.apply("hbase", conf)
    //val result = hbaseSM.getByKey[ResObj]("1_1_9999")
    val result = hbaseSM.delByKey("1_1_9999")
    println(result)
    val result2 = hbaseSM.getByKey[ResObj]("1_1_9999")
    println(result2)
  }
}