package com.foofv.crawler.parse.scheme.nodes

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, HashMap,Map}

/**
 * Created by soledede on 2015/8/19.
 */
class PostNode extends SemanticNode {


  override def exec(el: AnyRef, obj: Map[String, AnyRef], p: Map[String, AnyRef],context: AnyRef,contextCache: Map[String, AnyRef]): AnyRef= {
    var parameter = new HashMap[String, AnyRef]
    if (hasChild()) {
      for (ch <- children) {
        if (ch.isInstanceOf[StringNode]) {
          val str = ch.asInstanceOf[StringNode]
          val stresult = str.exec(null, null, parameter,context,contextCache)
        } else logWarning("this node just can use [str]")
      }
    }
    if (parameter.size <= 0) parameter = null
    parameter
  }
}
