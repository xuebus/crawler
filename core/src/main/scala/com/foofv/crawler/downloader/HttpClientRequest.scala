package com.foofv.crawler.downloader

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.BasicHttpContext
import org.codehaus.jackson.map.ObjectMapper
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.enumeration.HttpRequestMethodType
import com.foofv.crawler.util.constant.Constant
import com.foofv.crawler.util.http.HttpAsyncClientUtil
import com.foofv.crawler.util.http.HttpRequstUtil
import scala.actors.threadpool.AtomicInteger

class HttpClientRequest private (conf: CrawlerConf) extends HttpRequest {

  def download(taskEntity: CrawlerTaskEntity, callback: (HttpClientContext, org.apache.http.HttpResponse) => Unit): Unit = {
    try {
      val methodType = taskEntity.httpmethod
      val url = taskEntity.taskURI
      val taskId = taskEntity.taskId
      val request = HttpRequstUtil.createRequest(methodType, url)
        HttpRequstUtil.addCookies(request, taskEntity.cookies)
        HttpRequstUtil.setUserAgent(request, taskEntity.userAgent)
        HttpRequstUtil.setRefer(request, taskEntity.httpRefer)

      taskEntity.httpmethod match {
        case HttpRequestMethodType.POST => {
          if (taskEntity.parentTaskToParames.length() > 0) {
            val formparams: java.util.List[BasicNameValuePair] = new java.util.ArrayList[BasicNameValuePair]()
            val om: ObjectMapper = new ObjectMapper()
            val rootJsonNode = om.readTree(taskEntity.parentTaskToParames)
            val parentNode = rootJsonNode.getFields
            if (parentNode.hasNext()) {
              val paramsDataJsonNode = rootJsonNode.get(Constant(conf).PARAMSPATA)
              val it = paramsDataJsonNode.getFields
              while (it.hasNext()) {
                val key = it.next().getKey
                val value = paramsDataJsonNode.get(key)
                if (key != null && value != null) {
                  formparams.add(new BasicNameValuePair(key, value.getValueAsText))
                }
              }
            }

            /* val wmpoiIdStr = paramsDataJsonNode.get("wmpoiIdStr").getValueAsText
        val offset = paramsDataJsonNode.get("offset").getValueAsText
        val has_content = paramsDataJsonNode.get("has_content").getValueAsText
        val score_grade = paramsDataJsonNode.get("score_grade").getValueAsText
        
       
        formparams.add(new BasicNameValuePair("wmpoiIdStr", wmpoiIdStr))
        formparams.add(new BasicNameValuePair("offset", offset))
        formparams.add(new BasicNameValuePair("has_content", has_content))
        formparams.add(new BasicNameValuePair("score_grade", score_grade))*/
            val entity: UrlEncodedFormEntity = new UrlEncodedFormEntity(formparams, "utf-8");
            request.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
          } else {
            logWarning("post no parameter!" + taskEntity.taskURI)
          }
        }
        case _ => {}
      }

      // http requset context
      val context: HttpClientContext = HttpClientContext.adapt(new BasicHttpContext);
      context.setAttribute(HttpDownloader.TaskEntityId_Key, taskEntity.taskId)
      var reqCfgBuilder = RequestConfig.custom()
      if (taskEntity.isUseProxy == 1 && !taskEntity.proxyHost.trim().equalsIgnoreCase("null")) {
        val proxy = getHttpProxy(taskEntity.proxyHost, taskEntity.proxyPort)
        if (proxy != null) {
          context.setAttribute(Constant.CRAWLEr_AGENT_WORKER_HTTP_CONTEXT_PROXY_HOST, taskEntity.proxyHost + "_" + taskEntity.proxyPort)
          reqCfgBuilder = reqCfgBuilder.setProxy(proxy)
        }
      }
      reqCfgBuilder.setRedirectsEnabled(false)
      context.setRequestConfig(reqCfgBuilder.build())
      val startTime = System.currentTimeMillis()
      // Http request start time 
      context.setAttribute(HttpDownloader.TaskEntity_Starttime, System.currentTimeMillis())
      val taskNo = HttpClientRequest.taskNo.getAndIncrement
      context.setAttribute(Constant.CRAWLER_AGENT_WORKER_TASK_NO, taskNo)
      HttpAsyncClientUtil.getInstance(conf).execute(request, context, callback)
      val usingIP = taskEntity.taskIp
      logInfo(s"taskId[$taskId] url[$url] taskNo[$taskNo] execute... usingIP[$usingIP]")
    } catch {
      case t: Throwable => logError("http download faild!", t)
    }

  }

  def download(taskEntity: CrawlerTaskEntity): Response = {
    null.asInstanceOf[Response]
  }

}

object HttpClientRequest {
  var taskNo = new AtomicInteger
  taskNo.getAndIncrement
  
  var httpClientRequest: HttpClientRequest = null
  def apply(conf: CrawlerConf): HttpClientRequest = {
    if (httpClientRequest == null) httpClientRequest = new HttpClientRequest(conf)
    httpClientRequest
  }
}


