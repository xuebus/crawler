package com.foofv.crawler.parse.scheme.nodes

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, HashMap, Map}

/**
 * Created by soledede on 2015/8/19.
 */
private[crawler]
class StringNode extends SemanticNode {


  override def exec(el: AnyRef, obj: Map[String, AnyRef], p: Map[String, AnyRef], context: AnyRef, contextCache: Map[String, AnyRef]): AnyRef = {
    if (value != null && name != null) {
      if (!notSave) {
        obj(name) = value
      }
      if (needCache128) {
        if (value.length <= 128) contextCache(name) = value
        else logWarning("the length of your context cache content more than 128,we will not save it")
      } else if (needCache) contextCache(name) = value
      if (parameter) p(name) = value
      //if (name.toLowerCase.equalsIgnoreCase("total")) total = value.toInt
    }
    value
  }
}
