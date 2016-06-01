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
package com.foofv.crawler.entity

import java.util.concurrent.atomic.AtomicInteger
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import com.foofv.crawler.util.Logging
import com.foofv.crawler.rule.ITaskFilter
import org.apache.commons.lang3.StringUtils
import scala.collection.mutable.HashSet

/**
 * @author soledede
 * @email wengbenjue@163.com
 * receive input for web
 * one job has one or more tasks
 * jobType eg. theme crawler or general crawler or specify the url ...
 * taskfilter,filter the negative url ,you can cache job in master node, then work node get and cache it from master node
 */
private[crawler] class Job(
    var jobName: String
    //var taskfilter: ITaskFilter = null
  ) extends Logging {

  var jobId: Long = Job.geneJobId
  
  val crawlerTaskEntityList: Seq[CrawlerTaskEntity] = null
  
  val domainSet: HashSet[String] = new HashSet[String]
  
  def this() = {
    this(null)
  }
  
  override def toString(): String = {
    ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE)
  }

}

private[crawler] object Job {
  //auto increment jobId
  def geneJobId = System.currentTimeMillis()
}
