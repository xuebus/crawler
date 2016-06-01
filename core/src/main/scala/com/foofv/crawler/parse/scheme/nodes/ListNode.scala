package com.foofv.crawler.parse.scheme.nodes

import java.util
import java.util.Collections
import scala.collection.mutable._
import com.alibaba.fastjson.{JSON, JSONArray}
import com.foofv.crawler.parse.scheme.{JsonParser, FastJsonImpl}
import com.foofv.crawler.util.Util
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.control.Breaks._
import org.jsoup.Jsoup


/**
 * Created by soledede on 2015/8/18.
 */
class ListNode extends SemanticNode {

  var isFiled: Boolean = false


  def getIsFiled(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("isfield").trim
    if (i.isEmpty) i = element.getAttribute("isField").trim
    if (!i.isEmpty) {
      isFiled = i.toBoolean
    }
  }


  private def has(obj: HashMap[String, AnyRef]): Boolean = {
    var flag = false
    if (filterNot != null) {
      breakable {
        obj.foreach { x =>
          if (Util.find(x._2.asInstanceOf[String], filterNot)) {
            flag = true
            break
          }
        }
      }
    }
    flag
  }

  override def exec(el: AnyRef, result: Map[String, AnyRef], p: Map[String, AnyRef], context: AnyRef, contextCache: Map[String, AnyRef]): AnyRef = {
    if (cssSelector == null && jsonSelector == null) return null
    var obj = new HashMap[String, AnyRef] //return object to save
    var parameter = new HashMap[String, AnyRef] //return paramter to request next web page
    var resultList = new ListBuffer[Map[String, AnyRef]]
    var parameterList = new ListBuffer[(Map[String, AnyRef],Map[String, AnyRef])]
    var contextCacheList = new ListBuffer[Map[String, AnyRef]]
    var contextCache = new HashMap[String, AnyRef]

    /* var total = -1*/

    var jElements: JSON = null
    var element: Element = null
    var elements: Elements = null
    var tmpElements: Elements = null
    var document: org.jsoup.nodes.Document = null
    
    var elCp = el
    if (regex != null) elCp = Util.regexExtract(el.toString, regex, group).asInstanceOf[String]

    var contextElements: org.jsoup.select.Elements = null
    if (useContext && context.isInstanceOf[org.jsoup.nodes.Document]) {
      contextElements = context.asInstanceOf[org.jsoup.nodes.Document].getAllElements
    }
    else if (elCp.isInstanceOf[JSON]) jElements = elCp.asInstanceOf[JSON]
    else if (elCp.isInstanceOf[Element]) {
      element = elCp.asInstanceOf[Element]
    }
    else if (elCp.isInstanceOf[org.jsoup.nodes.Document]) {
      elements = elCp.asInstanceOf[org.jsoup.nodes.Document].getAllElements
    } else if (elCp.isInstanceOf[String]) {
      try {
        document = Jsoup.parse(elCp.asInstanceOf[String])
      } catch {
        case t: Exception =>
      }
    }
    else elements = elCp.asInstanceOf[Elements]



    if (cssSelector != null && !cssSelector.isEmpty) {
      if (contextElements != null) {
        tmpElements = contextElements.select(cssSelector)
      }
      else if (element != null) {
        tmpElements = element.select(cssSelector)
      }else if(document != null){
        tmpElements = document.select(cssSelector)
      } else {
        tmpElements = elements.select(cssSelector)
      }
      if (hasChild()) {
        for (o <- 0 to tmpElements.size() - 1) {
          for (i <- 0 to this.children.size - 1) {
            //var childNode = null
            this.children(i).exec(tmpElements.get(o), obj, parameter, context, contextCache)
            /* if (childNode._2 > 0) total = childNode._2*/
          }
          reset
        }
      }

    } else if (jsonSelector != null && !jsonSelector.isEmpty) {
      if (el.isInstanceOf[JSON]) {
        val jsonParser: JsonParser = new FastJsonImpl(el.asInstanceOf[JSON]).select(jsonSelector)
        if (jsonParser.value().isInstanceOf[JSONArray]) {
          val jArray: JSONArray = jsonParser.value().asInstanceOf[JSONArray]
          val it: util.ListIterator[AnyRef] = jArray.listIterator()
          while (it.hasNext) {
          val o = it.next()
          if (hasChild()) {
            for (ch: SemanticNode <- children) {
              val jNodeT = ch.exec(o, obj, parameter, context, contextCache)
              /*if (jNodeT._2 > 0) total = jNodeT._2*/
            }
          }
          reset
          }
        }

      }

    }
    def reset: Unit = {
      var flag: Boolean = false
      var flagPar: Boolean = false
      var flagCache: Boolean = false
      if (!obj.isEmpty) {
        flag = has(obj)
      }
      if (!parameter.isEmpty) {
        flagPar = has(parameter)
      }
      if (!contextCache.isEmpty)
        flagCache = has(contextCache)
      if (!flag && !flagPar && !flagCache) {
        if (!obj.isEmpty)
          resultList.append(obj)
        if(contextCache.isEmpty) contextCache = null
        if (!parameter.isEmpty)
          parameterList.append((parameter,contextCache))
      }
      obj = new mutable.HashMap[String, AnyRef]
      parameter = new mutable.HashMap[String, AnyRef]
      contextCache = new mutable.HashMap[String, AnyRef]
    }
    if (resultList.size <= 0) resultList = null
    if (parameterList.size <= 0) parameterList = null
    if (contextCacheList.size <= 0) contextCacheList = null
    if (resultList == null && parameterList == null) return null
    if (notSave) resultList = null
    (resultList, parameterList,name)
  }


}
