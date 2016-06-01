package com.foofv.crawler.downloader

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods.HttpGet
import com.foofv.crawler.enumeration.{ CrawlerTaskType, CrawlerTaskFetchStatus, CrawlerTaskRuleUpdateType, HttpRequestMethodType }

import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.CrawlerConf

object Test {
  
  def main(args: Array[String]): Unit = {
    testTaskRunner()
  }
  
  def testTaskRunner(){
    var taskEntity: CrawlerTaskEntity = testTaskEntity()
    var downloader: Downloader = Downloader("http",new CrawlerConf())
    val tt = new TaskRunner(taskEntity, downloader)
    tt.run()
  }
  
  
  def testTaskEntity(): CrawlerTaskEntity = {
    val task = new CrawlerTaskEntity
    task.parentTaskId = null
    task.parentTaskToParames = null
    task.jobId = 1
    task.jobName = "job.jobName"
    task.taskType = CrawlerTaskType.THEME_CRAWLER
    task.taskURI = "http://waimai.meituan.com/?utm_campaign=baidu&utm_source=4204"
    task.taskDomain = "domain ----------- test"
    task.cookies = "cookies ----------- test"
    task.isUseProxy = 0
    task.ruleUpdateType = CrawlerTaskRuleUpdateType.AUTO_INTELLIGENCE
    task.totalDepth = 1
    task.intervalTime = 10
    task.httpmethod = HttpRequestMethodType.GET
    task
  }

}