package com.foofv.crawler.util.http

import org.apache.http.client.methods.HttpRequestBase
import com.foofv.crawler.enumeration.HttpRequestMethodType
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpGet
import org.apache.commons.lang3.StringUtils

private[crawler]
object HttpRequstUtil {
  
  val DefalutUserAgent = "Mozilla/5.0 (Windows NT 8.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.99 Safari/537.36"
  
  def createRequest(methodType: HttpRequestMethodType.Type, uri: String): HttpRequestBase = {
    
    //initialize http request
    var httpReq: HttpRequestBase = methodType match {
      case HttpRequestMethodType.POST => new HttpPost(uri)
      case _                          => new HttpGet(uri)
    }
    // initialize http request headers
    httpReq.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    httpReq.addHeader("Accept-Charset", "utf-8")
    //httpReq.addHeader("Accept-Encoding", "gzip, deflate, sdch")
    httpReq.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    httpReq.addHeader("Connection", "close")
    httpReq.addHeader("Cache-Control", "max-age=0")
    httpReq.addHeader("User-Agent", DefalutUserAgent)
    httpReq
  }
  
  def addCookies(request: HttpRequestBase, cookies: String) = HttpRequstUtil.addHeader(request, "Cookie", cookies)
  
  def setUserAgent(request: HttpRequestBase, userAgent: String) = HttpRequstUtil.addHeader(request, "User-Agent", userAgent)
  
  def setRefer(request: HttpRequestBase, refer: String) = HttpRequstUtil.addHeader(request, "Refer", refer)
  
  def addHeader(request: HttpRequestBase, key: String, value: String) = {
    if (StringUtils.isNotBlank(value)) {
      val header = request.getFirstHeader(key)
      if (header != null) {
        request.removeHeaders(key)
      }
    	request.addHeader(key, value.trim())
    }
  }
  
}