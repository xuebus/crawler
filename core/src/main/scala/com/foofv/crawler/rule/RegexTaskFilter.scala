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
package com.foofv.crawler.rule

import scala.collection.mutable.HashSet
import com.foofv.crawler.util.Logging
import com.foofv.crawler.CrawlerConf
import scala.util.matching.Regex
import scala.util.control.Breaks._

/**
 * The filter of URL ,You can use Regex, like this
 * @author soledede
 */
private[crawler] class RegexTaskFilter(conf: CrawlerConf, taskFilter: ITaskFilter) extends ITaskFilter with Logging {

  def this(conf: CrawlerConf) = this(conf, null)
  def this() = this(null, null)

  val rules = new HashSet[Regex]

  override def addRule(rule: String): ITaskFilter = {
    this.rules += rule.r
    this
  }

  override def delRule(rule: String): ITaskFilter = {
    this.rules -= rule.r
    this
  }
  override def matchRule(target: String): Boolean = {
    var matched = true
    var currendMatch = false
    if (taskFilter != null) {
      matched = taskFilter.matchRule(target)
    }
    breakable {
      for (r <- rules) {
        currendMatch = r.pattern.matcher(target).find()
        if (currendMatch)
          break
      }
    }
    matched && currendMatch
  }
}

object RegexTaskFilter {
  def main(args: Array[String]): Unit = {
    println("come in..")
   // val r = "[a-zA-Z]".r
    /* "hello" match {
      case r => println(r)
    }*/
    //println(r.pattern.matcher("lA").find())
    //println(r.pattern.matcher("12345werD").matches())
    
    val solededeR = "http://(www.)*soledede.com|cn"
    println(new RegexTaskFilter().addRule(solededeR).matchRule("http://soledede.com/"))
    
    
  }
}

