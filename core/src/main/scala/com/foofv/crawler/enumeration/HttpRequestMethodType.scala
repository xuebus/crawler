package com.foofv.crawler.enumeration

/**
 * The type of Http Request Method
 */
private[crawler] object HttpRequestMethodType  extends Enumeration{
  type Type = Value 
  val GET = Value("get")
  val POST = Value("post")
}