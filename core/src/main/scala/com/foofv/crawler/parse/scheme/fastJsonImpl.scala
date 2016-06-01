package com.foofv.crawler.parse.scheme

import com.alibaba.fastjson.{JSONArray, JSONObject, JSON}

/**
 * Created by soledede on 2015/8/19.
 */
private[crawler]
class FastJsonImpl(json: JSON, jsonString: String) extends JsonParser {

  def this(jsonString: String) = this(null, jsonString)

  def this(json: JSON) = this(json, null)

  var jobj: JSON = null

  if (jsonString != null) jobj = JSON.parse(jsonString).asInstanceOf[JSON]
  else jobj = json

  var jValue: AnyRef = jobj

  override def value(): AnyRef = {
    jValue
  }

  override def select(selector: String): JsonParser = {
    if (value != null && selector != null && !selector.isEmpty) {
      val fileds = selector.split("->")
      if (fileds.length > 0) {
        var i: Int = 0
        while (i < fileds.length) {
          if (value.isInstanceOf[JSONObject])
            jValue = jValue.asInstanceOf[JSONObject].get(fileds(i))
          i += 1
        }
      } else {
        if (value.isInstanceOf[JSONObject])
          jValue = jValue.asInstanceOf[JSONObject].get(selector)
      }
    }
    this
  }

  override def text(): String = {
    var result: String = null
    if (value != null) {
      if (value.isInstanceOf[JSONObject]) result = value.asInstanceOf[JSONObject].toJSONString
      else if (value.isInstanceOf[JSONArray]) result = value.asInstanceOf[JSONArray].toJSONString
      else if (value.isInstanceOf[String]) result = value.asInstanceOf[String]
      else result = value.toString
    }
    result
  }
}

object FastJsonImpl {


  def apply(jsonString: String): JsonParser = {
    new FastJsonImpl(jsonString)
  }
}
