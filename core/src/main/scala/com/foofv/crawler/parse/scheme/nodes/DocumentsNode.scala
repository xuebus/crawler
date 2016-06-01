package com.foofv.crawler.parse.scheme.nodes

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, HashMap,Map}

/**
 * Created by soledede on 2015/8/19.
 */
private[crawler]
class DocumentsNode extends SemanticNode {

  var tableName: String = "sys_soledede"

  def getTable(element: org.w3c.dom.Element): String = {
    tableName = element.getAttribute("table").trim
    if (tableName.isEmpty) tableName = "sys_soledede"
    tableName
  }

  override def exec(el: AnyRef, obj: Map[String, AnyRef], p: Map[String, AnyRef],context: AnyRef,contextCache: Map[String, AnyRef]): (AnyRef, Int) = null
}
