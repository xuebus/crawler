package com.foofv.crawler.parse.scheme.nodes

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, HashMap,Map}

/**
 * Created by soledede on 2015/8/19.
 */
private[crawler]
class ReferenceNode extends SemanticNode {

  var docId: String = null

  def getDocId(element: org.w3c.dom.Element) = {
    docId = element.getAttribute("docid").trim
    if (docId.isEmpty) docId = element.getAttribute("docId").trim
    if (docId.isEmpty) docId = null
  }

  override def exec(el: AnyRef, obj: Map[String, AnyRef], p: Map[String, AnyRef],context: AnyRef,contextCache: Map[String, AnyRef]): AnyRef = {
    docId
  }
}
