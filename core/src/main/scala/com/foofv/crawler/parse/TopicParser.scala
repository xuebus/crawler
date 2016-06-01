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
package com.foofv.crawler.parse

import com.foofv.crawler.rule.ITaskFilter
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.util.listener.ManagerListenerWaiter

abstract class TopicParser extends Parser {


  private val random = new java.util.Random()

  // get random milliseconds, value range:[1, 3*60*1000]
  protected def getChildTaskIntervalTime: Long = {
    random.nextInt(3 * 10 * 1000) + 1
  }

  override def parse(resObj: ResObj, obj: AnyRef): AnyRef = {

    var result: AnyRef = null
    try {
      val taskEntity = resObj.tastEntity
      result = taskEntity.currentDepth match {
        case 1  => parse1DepthPage(resObj, obj)
        case 2  => parse2DepthPage(resObj, obj)
        case 3  => parse3DepthPage(resObj, obj)
        case 4  => parse4DepthPage(resObj, obj)
        case 5  => parse5DepthPage(resObj, obj)
        case 6  => parse6DepthPage(resObj, obj)
        case 7  => parse7DepthPage(resObj, obj)
        case 8  => parse8DepthPage(resObj, obj)
        case 9  => parse9DepthPage(resObj, obj)
        case 10 => parse10DepthPage(resObj, obj)
        case _  => null
      }
    } catch {
      case t: Throwable => logError("parse error", t)
    }
    result
  }

  override def parse(resObj: ResObj): AnyRef = {
    var result: AnyRef = null
    try {
      val taskEntity = resObj.tastEntity
      result = taskEntity.currentDepth match {
        case 1  => parse1DepthPage(resObj)
        case 2  => parse2DepthPage(resObj)
        case 3  => parse3DepthPage(resObj)
        case 4  => parse4DepthPage(resObj)
        case 5  => parse5DepthPage(resObj)
        case 6  => parse6DepthPage(resObj)
        case 7  => parse7DepthPage(resObj)
        case 8  => parse8DepthPage(resObj)
        case 9  => parse9DepthPage(resObj)
        case 10 => parse10DepthPage(resObj)
        case _  => null
      }
    } catch {
      case t: Throwable => logError("parse error", t)
    }
    result
  }

  protected def parse1DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse2DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse3DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse4DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse5DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse6DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse7DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse8DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse9DepthPage(resObj: ResObj): AnyRef = { null }
  protected def parse10DepthPage(resObj: ResObj): AnyRef = { null }

  protected def parse1DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse2DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse3DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse4DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse5DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse6DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse7DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse8DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse9DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }
  protected def parse10DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = { null }

  override def parseYourSelf(resObj: ResObj, obj: AnyRef) = {
    try {
      val taskEntity = resObj.tastEntity
      taskEntity.currentDepth match {
        case 1  => parse1DepthPageYourself(resObj, obj)
        case 2  => parse2DepthPageYourself(resObj, obj)
        case 3  => parse3DepthPageYourself(resObj, obj)
        case 4  => parse4DepthPageYourself(resObj, obj)
        case 5  => parse5DepthPageYourself(resObj, obj)
        case 6  => parse6DepthPageYourself(resObj, obj)
        case 7  => parse7DepthPageYourself(resObj, obj)
        case 8  => parse8DepthPageYourself(resObj, obj)
        case 9  => parse9DepthPageYourself(resObj, obj)
        case 10 => parse10DepthPageYourself(resObj, obj)
        case _  =>
      }
    } catch {
      case t: Throwable => logError("parse error", t)
    }
  }

  override def parseYourSelf(resObj: ResObj) = {
    try {
      val taskEntity = resObj.tastEntity
      taskEntity.currentDepth match {
        case 1  => parse1DepthPageYourself(resObj)
        case 2  => parse2DepthPageYourself(resObj)
        case 3  => parse3DepthPageYourself(resObj)
        case 4  => parse4DepthPageYourself(resObj)
        case 5  => parse5DepthPageYourself(resObj)
        case 6  => parse6DepthPageYourself(resObj)
        case 7  => parse7DepthPageYourself(resObj)
        case 8  => parse8DepthPageYourself(resObj)
        case 9  => parse9DepthPageYourself(resObj)
        case 10 => parse10DepthPageYourself(resObj)
        case _  =>
      }
    } catch {
      case t: Throwable => logError("parse error", t)
    }
  }

  protected def parse1DepthPageYourself(resObj: ResObj) = { null }
  protected def parse2DepthPageYourself(resObj: ResObj) = { null }
  protected def parse3DepthPageYourself(resObj: ResObj) = { null }
  protected def parse4DepthPageYourself(resObj: ResObj) = { null }
  protected def parse5DepthPageYourself(resObj: ResObj) = { null }
  protected def parse6DepthPageYourself(resObj: ResObj) = { null }
  protected def parse7DepthPageYourself(resObj: ResObj) = { null }
  protected def parse8DepthPageYourself(resObj: ResObj) = { null }
  protected def parse9DepthPageYourself(resObj: ResObj) = { null }
  protected def parse10DepthPageYourself(resObj: ResObj) = { null }

  protected def parse1DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse2DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse3DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse4DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse5DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse6DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse7DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse8DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse9DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }
  protected def parse10DepthPageYourself(resObj: ResObj, obj: AnyRef) = { null }

  override def geneNewTaskEntitySet(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    var result: (Seq[CrawlerTaskEntity], AnyRef) = null
    try {
      val taskEntity = resObj.tastEntity
      result = taskEntity.currentDepth match {
        case 1  => geneNewTaskEntitySet1(resObj)
        case 2  => geneNewTaskEntitySet2(resObj)
        case 3  => geneNewTaskEntitySet3(resObj)
        case 4  => geneNewTaskEntitySet4(resObj)
        case 5  => geneNewTaskEntitySet5(resObj)
        case 6  => geneNewTaskEntitySet6(resObj)
        case 7  => geneNewTaskEntitySet7(resObj)
        case 8  => geneNewTaskEntitySet8(resObj)
        case 9  => geneNewTaskEntitySet9(resObj)
        case 10 => geneNewTaskEntitySet10(resObj)
        case _  => (null,null)
      }

    } catch {
      case t: Throwable => logError("geneNewTaskEntitySet error", t)
    }
   // if(result._2==null) return (null,new AnyRef)
    result
  }

  protected def geneNewTaskEntitySet1(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet2(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet3(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet4(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet5(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet6(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet7(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet8(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet9(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }
  protected def geneNewTaskEntitySet10(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = { null }

}