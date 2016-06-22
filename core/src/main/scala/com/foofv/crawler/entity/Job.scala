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
