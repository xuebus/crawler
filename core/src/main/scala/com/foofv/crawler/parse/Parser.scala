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
package com.foofv.crawler.parse

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.rule.ITaskFilter
import com.foofv.crawler.rule.ITaskFilterFactory
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.schedule.ITaskQueueFactory
import com.foofv.crawler.util.Logging
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.parse.xpath.HtmlSoldier
import com.foofv.crawler.enumeration._

import com.google.common.cache._
import java.util.concurrent.TimeUnit
import scala.util.control.Breaks._

/**
 * the primary parser object
 * @author soledede
 */
private[crawler] abstract class Parser extends Logging {
  var analyzer: Analyzer = null

  //we can help you save you object with our framework
  def parse(resObj: ResObj, obj: AnyRef): AnyRef = {
    null
  }

  def parse(resObj: ResObj): AnyRef = {
    null
  }

  //save your object by yourself , you can choice this
  def parseYourSelf(resObj: ResObj, obj: AnyRef): Unit = {}

  def parseYourSelf(resObj: ResObj): Unit = {}

  //liking page number > 1,we need put the crawlerTaskEntity into the schedule again
  //liking when we are in first level now,we need put the crawlerTaskEntity into the schedule again
  def geneNewTaskEntitySet(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    null
  }

  //you need extract the url of sub web pages when it's not the last page 
  private def extractSubCrawlerTaskEntitySet(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    null
  }

}

private[crawler] object Parser extends Logging {

  private val cacheLoader: CacheLoader[java.lang.String, (Boolean, Parser)] =
    new CacheLoader[java.lang.String, (Boolean, Parser)]() {
      def load(key: java.lang.String): (Boolean, Parser) = {
        null.asInstanceOf[(Boolean, Parser)]
      }
    }

  // Parser instances cache (Key[taskEntity.topciCrawlerParserClassName]->Value[(Boolean, Parser)])
  private val parserCacheManager = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(cacheLoader)

  //update Parser cache,  set Parser intance invalide
  def setParserInstanceInvalide(topciCrawlerParserClassName: String) {
    // get Parser from cache
    var parser: Parser = null
    val parserCache = parserCacheManager.getIfPresent(topciCrawlerParserClassName)
    if (parserCache != null) {
      parser = parserCache._2
    }
    // set Parser intance invalide
    parserCacheManager.put(topciCrawlerParserClassName, (false, parser))
  }

  // get Parser instance
  // first, getIfPresent by taskEntity.topciCrawlerParserClassName from Parser.parserCacheManager;
  // if null, create Parser instance by reflect with taskEntity.topciCrawlerParserClassName
  def getParser(taskEntity: CrawlerTaskEntity, conf: CrawlerConf): Parser = {
    taskEntity.taskType match {
      case CrawlerTaskType.GENERAL_CRAWLER => null //general crawler //TODO
      case CrawlerTaskType.THEME_CRAWLER => {
        //theme crawler
        var parser: Parser = null
        val topciCrawlerParserClassName = taskEntity.topciCrawlerParserClassName
        // get Parser from cache
        val parserCache = parserCacheManager.getIfPresent(topciCrawlerParserClassName)
        if (parserCache != null) {
          // get Parser from Tuple(Boolean, Parser)
          // Boolean is true, return Tuple._2
          // if false, return null. Job has been cancelled and given up. Do Not continue.
          if (parserCache._1 == false) {
            val jobName: String = taskEntity.jobName
            logDebug(s"Job [$jobName] has been cancelled and given up. Do Not continue.")
          } else {
            parser = parserCache._2
            // update Parser cache, keep alive
            parserCacheManager.put(topciCrawlerParserClassName, parserCache)
          }
        } else {
          parser = getParserInstanceByReflect(taskEntity, conf)
          // update Parser cache
          parserCacheManager.put(topciCrawlerParserClassName, (true, parser))
        }
        parser
      }
      case _ => null
    }
  }

  // create Parser instance by reflect
  private def getParserInstanceByReflect(taskEntity: CrawlerTaskEntity, conf: CrawlerConf): Parser = {
    var parser: Parser = null
    var parserClassName = ""
    try {
      parserClassName = taskEntity.topciCrawlerParserClassName
      val con = Class.forName(parserClassName).getDeclaredConstructors
      if (con.length > 0) {
        breakable {
          for (c <- 0 to con.length-1) {
            val cP = con(c).getParameterCount
            if (cP > 0) {
              parser = con(c).newInstance(conf).asInstanceOf[Parser]
              break
            }
            else{
              parser = con(c).newInstance().asInstanceOf[Parser]
              break
            }
          }
        }
      }else parser = Class.forName(parserClassName).newInstance().asInstanceOf[Parser]
      //parser = .newInstance().asInstanceOf[Parser]
      logDebug(s" create parser [$parserClassName] instance success")
    } catch {
      case t: Throwable => logError(s"create parser [$parserClassName] instance Failed ", t)
    }
    parser
  }

  private def getParserIfValid(jobName: String, parserCache: (Boolean, Parser)): Parser = {
    var parser: Parser = null
    if (parserCache._1 == false) {
      logDebug(s"Job [$jobName] has been cancelled and given up. Do Not continue.")
    } else {
      parser = parserCache._2
    }
    parser
  }

}

private[crawler] trait ParserFactory {
  def currentParser(resObj: ResObj,conf: CrawlerConf): Parser
}

class TestParser extends Parser {

  override def parse(resObj: ResObj, obj: AnyRef): AnyRef = {
    println(resObj.tastEntity.toString())
    null.asInstanceOf[AnyRef]
  }

}


