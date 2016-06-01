package com.foofv.crawler.util.http

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.protocol.HttpContext
import org.apache.http.concurrent.FutureCallback
import org.apache.http.HttpResponse
import org.apache.http.nio.client.util.HttpAsyncClientUtils
import org.apache.http.nio.reactor.ConnectingIOReactor
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.HttpHost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.HttpEntity
import org.apache.http.util.EntityUtils
import com.foofv.crawler.enumeration.HttpRequestMethodType
import com.foofv.crawler.util.Logging
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.nio.reactor.IOReactorConfig
import com.foofv.crawler.CrawlerConf
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ArrayBuffer
import com.foofv.crawler.agent.AgentWorker
import com.foofv.crawler.util.constant.Constant
import scala.actors.threadpool.AtomicInteger
import com.foofv.crawler.downloader.HttpDownloader
import com.foofv.crawler.schedule.ITaskQueue

private[crawler] class HttpAsyncClientUtil private(conf: CrawlerConf) extends Logging {

  val ioThreadCount = conf.getInt("", 2)
  val config: IOReactorConfig = IOReactorConfig.custom()
/*    .setTcpNoDelay(true)
    .setSoReuseAddress(false)
    //.setConnectTimeout(0)
    //.setIoThreadCount(ioThreadCount)
    .setRcvBufSize(1024 * 5)
    .setBacklogSize(2000)
    .setSoKeepAlive(false)
    .setInterestOpQueued(true)*/
    .setSoReuseAddress(false)
    .setIoThreadCount(ioThreadCount)
    .setSoTimeout(60*1000)
    .setRcvBufSize(1024 * 64)
    .build();
  private val ioReactor: ConnectingIOReactor = new DefaultConnectingIOReactor(config)
  private val cm: PoolingNHttpClientConnectionManager = new PoolingNHttpClientConnectionManager(ioReactor)
  cm.setDefaultMaxPerRoute(10)
  cm.setMaxTotal(2000)

  private val httpClient: CloseableHttpAsyncClient = HttpAsyncClients.custom().setConnectionManager(cm).build();
  httpClient.start()

  // use Proxy
  def execute(request: HttpRequestBase, proxy: HttpHost, callback: (HttpClientContext, HttpResponse) => Unit): Unit = {
    val context: HttpClientContext = HttpClientContext.adapt(new BasicHttpContext);
    val reqCfg = RequestConfig.custom().setProxy(proxy).build();
    context.setRequestConfig(reqCfg)
    this.execute(request, context, callback)
  }

  def execute(request: HttpRequestBase, context: HttpClientContext, callback: (HttpClientContext, HttpResponse) => Unit): Unit = {

    try {
      logDebug("HttpAsyncClientUtil execute begin")
      if (context == null) {
        httpClient.execute(request, new FutureCallbackAdapter(callback))
      } else {
        httpClient.execute(request, context, new FutureCallbackAdapter(context, callback))
      }
    } catch {
      case t: Throwable => {
        logError("HttpAsync Error", t)
      }
    }
  }

  // default
  def execute(request: HttpRequestBase, callback: (HttpClientContext, HttpResponse) => Unit): Unit = {
    this.execute(request, null.asInstanceOf[HttpClientContext], callback)
  }

}

private[crawler] object HttpAsyncClientUtil {
  
  var callbackFailedCounter = new AtomicInteger
  callbackFailedCounter.getAndIncrement

  private var instance: HttpAsyncClientUtil = _

  def getInstance(conf: CrawlerConf): HttpAsyncClientUtil = {
    if (instance == null) {
      instance = new HttpAsyncClientUtil(conf)
    }
    return instance
  }

  def closeHttpClient() = HttpAsyncClientUtils.closeQuietly(instance.httpClient);

}

private[crawler] class FutureCallbackAdapter(var httpContext: HttpClientContext, val callback: (HttpClientContext, HttpResponse) => Unit) extends FutureCallback[HttpResponse] with Logging {

  def this(callback: (HttpClientContext, HttpResponse) => Unit) = {
    this(null, callback)
  }

  def cancelled(): Unit = {
    logWarning("Http Requst cancelled")
  }

  def completed(httpResp: HttpResponse): Unit = {
    try {
      //      val entity = httpResp.getEntity
      //      val respContent = EntityUtils.toString(entity)
      //      println(respContent)
      callback(httpContext, httpResp)
    } catch {
      case t: Throwable => logError("Http Requst callback Error", t)
    } finally {
      HttpClientUtils.closeQuietly(httpResp)
    }
  }

  def failed(e: Exception): Unit = {
    try {
      var httpData = ""
      if (httpContext != null) {
        httpData = httpContext.getRequest.toString()
      }
      var taskNo = ""
      var cnt = httpContext.getAttribute(Constant.CRAWLER_AGENT_WORKER_TASK_NO)
      if (cnt != null) {
        taskNo = cnt.toString()
      }
      val callbackFailedCounter = HttpAsyncClientUtil.callbackFailedCounter.getAndIncrement
    	logError(s"callbackFailedCounter[$callbackFailedCounter] taskNo[$taskNo] Http Requst [$httpData] callback failed", e)
      if (e.isInstanceOf[java.net.SocketTimeoutException] || e.isInstanceOf[java.net.SocketException]) {
        val proxyHost = httpContext.getAttribute(Constant.CRAWLEr_AGENT_WORKER_HTTP_CONTEXT_PROXY_HOST)
        AgentWorker.addTimeoutProxyRecord(proxyHost.toString())
      }
      val taskEntity = HttpDownloader.getTaskEntityFromHttpClientContext(httpContext)
      AgentWorker.putFailedTaskBacktoSortedSet(taskEntity)
    } catch {
      case t: Throwable => logError("Http Requst failed error", t)
    }
  }

}

private[crawler] object TestHttpAsyncClientUtil {
  def main(args: Array[String]): Unit = {
    testHttpAsync
  }

  def testHttpAsync = {
    var start: Long = -1
    var end: Long = -1
    val request = HttpRequstUtil.createRequest(HttpRequestMethodType.GET, "http://www.cnblogs.com/Fredric-2013/p/4417960.html")
    def callback(context: HttpClientContext, resp: HttpResponse) = {
      println(Thread.currentThread().getName)
      println(resp)
      end = System.currentTimeMillis()
      printf("start %15d, end %15d, cost %15d \n", start, end, end - start)
      start = System.currentTimeMillis()
    }
    start = System.currentTimeMillis()
    for (i <- 1 to 5) {
      HttpAsyncClientUtil.getInstance(new CrawlerConf).execute(request, callback)
    }
  }

  def testHttpPost = {
    var start: Long = -1
    var end: Long = -1
    val request = HttpRequstUtil.createRequest(HttpRequestMethodType.POST, "http://waimai.meituan.com/ajax/comment")
    def callback(context: HttpClientContext, resp: HttpResponse) = {
      println(Thread.currentThread().getName)
      println(resp)
      end = System.currentTimeMillis()
      printf("start %15d, end %15d, cost %15d \n", start, end, end - start)
      start = System.currentTimeMillis()
    }
    start = System.currentTimeMillis()
    val ctx: HttpClientContext = HttpClientContext.adapt(new BasicHttpContext);
    var reqCfgBuilder = RequestConfig.custom()
    reqCfgBuilder.setRedirectsEnabled(false)
    ctx.setRequestConfig(reqCfgBuilder.build())
    for (i <- 1 to 5) {
      new Thread {
        override def run() {
          HttpAsyncClientUtil.getInstance(new CrawlerConf).execute(request, ctx, callback _)
        }
      }.start()
    }
  }

}
