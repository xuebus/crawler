/**
  * Copyright [2015] [soleede]
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
package com.foofv.crawler.parse

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.util.Logging
import com.foofv.crawler.enumeration.NeedSaveParser
import com.foofv.crawler.storage.StorageManager

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Map}

/**
  * the thread of parser
  *
  * @author soledede
  */
private[crawler] class ParserPersistRunner(parser: Parser, resObj: ResObj, obj: AnyRef, conf: CrawlerConf) extends Runnable with Logging {

  override def run = {
    // isNeedSaveParserYslf is for user parser and save object by himself/herself
    resObj.tastEntity.isNeedSaveParserYslf match {
      case NeedSaveParser.NO_NEED =>
        //obj =null
        val retrunObj = parser.parse(resObj)
        if (retrunObj != null) saveEntity(retrunObj)
        else {
          //eg: (sougou_relate_words,ListBuffer(Map(word -> 中科三环), Map(word -> 长城信息), Map(word -> 北方稀土), Map(word -> 招商证券), Map(word -> 正海磁材), Map(word -> 宗申动力)))
          saveEntity(parser.parse(resObj, obj))
        }
      case NeedSaveParser.NEED => parser.parseYourSelf(resObj, obj)
    }
  }

  private def saveEntity(entity: AnyRef): Boolean = {
    if (entity == null) return false
    if (entity.isInstanceOf[Seq[Any]] && entity.asInstanceOf[Seq[Any]].size <= 0) return false
    /*if (entity.isInstanceOf[(String, mutable.Map[String, Any])]) {
      val mapObjs = entity.asInstanceOf[(String, mutable.Map[String, Any])]._2
      if (mapObjs == null || mapObjs.isEmpty) {
        logInfo("no schema object,We have returned......")
        return false
      }
  }*/
    logInfo("saveEntity " + entity)
    var r = false
    // TODO 
    try {
      var storage: StorageManager = null
      if(resObj.tastEntity.storage.equalsIgnoreCase("file")){
        storage = StorageManager("local", conf)
      }else{
        storage = StorageManager("mongo", conf)
      }
      if(storage==null){
        if (conf.getBoolean("local", false)) {
          storage = StorageManager("local", conf)
        } else {
          storage = StorageManager("mongo", conf)
        }
      }
      if (entity.isInstanceOf[Seq[_]]) {
        val list: Seq[_] = entity.asInstanceOf[Seq[_]]
        list.foreach(e => try {
          storage.put(e)
          r = true
        } catch {
          case t: Throwable => logError("save entity to mongo failed", t)
        })
      } else if (entity.isInstanceOf[(String, Seq[Map[String, AnyRef]])]) {
        try {
          //(tablename,List[Map[jobId->1,word->九州]])
          val tuEntity = entity.asInstanceOf[(String, Seq[Map[String, AnyRef]])]
          val tableName = tuEntity._1
          val oE = tuEntity._2
          storage.put(tableName, oE)
        } catch {
          case t: Throwable => logError("save entity(eg:tablename,List[Map[jobId->1,word->九州]])) to mongo failed", t)
        }
      } else {
        r = storage.put(entity)
      }
    } catch {
      case t: Throwable => logError("save entity to mongo failed", t)
    }

    if (!r) logError("save entity to storagemanager failed!")
    r
  }
}