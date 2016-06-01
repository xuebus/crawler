package com.foofv.crawler.parse.scheme

/**
 * Created by soledede on 2015/8/19.
 */
private[crawler]
trait JsonParser {

  def value():AnyRef

  def select(jsonString: String):JsonParser

  def text(): String
}
