package com.foofv.crawler.parse.scheme.nodes

import com.foofv.crawler.util.Util
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, HashMap, Map}

/**
 * Created by soledede on 2015/8/19.
 */
private[crawler]
class AttributeNode extends SemanticNode {

  var attributeName: String = null
  var index: Int = 0


  def getAttribute(element: org.w3c.dom.Element) {
    attributeName = element.getAttribute("attr").trim
    if (attributeName.isEmpty) {
      attributeName = null
    }
  }

  def getIndex(element: org.w3c.dom.Element) {
    val i: String = element.getAttribute("index").trim
    if (!i.isEmpty) {
      index = i.toInt
    }
  }


  override def exec(el: AnyRef, obj: Map[String, AnyRef], p: Map[String, AnyRef], context: AnyRef, contextCache: Map[String, AnyRef]): AnyRef = {
    // var total: Int = -1
    if (attributeName == null) {
      return null
    }
    var resultString: String = null
    var element: Element = null
    var elements: Elements = null
    var tmpElements: org.jsoup.select.Elements = null
    var contextElements: org.jsoup.select.Elements = null
    if (useContext && context.isInstanceOf[org.jsoup.nodes.Document]) {
      contextElements = context.asInstanceOf[org.jsoup.nodes.Document].getAllElements
    }
    else if (el.isInstanceOf[Element]) {
      element = el.asInstanceOf[Element]
    } else if (el.isInstanceOf[Elements]) {
      elements = el.asInstanceOf[Elements]
    }

    if (cssSelector != null && !cssSelector.isEmpty) {
      if (contextElements != null) {
        tmpElements = contextElements.select(cssSelector)
      }
      else if (element != null) {
        tmpElements = element.select(cssSelector)
      } else {
        tmpElements = element.select(cssSelector)
      }
    }

    if (tmpElements == null) {
      if (elements != null) tmpElements = elements
    }


    if (tmpElements == null && element != null) {
      val result: String = element.attr(attributeName)
      // if (name.toLowerCase.equalsIgnoreCase("total")) total = result.toInt
      resultString = result
    }
    else if (tmpElements != null && tmpElements.size() > 0) {
      var attValue: String = null
      try {
        var ele: Element = null
        if (last) ele = tmpElements.get(tmpElements.size() - 1)
        else ele = tmpElements.get(index)
        if (ele != null) {
          if (ele.hasAttr(attributeName)) attValue = ele.attr(attributeName)
          if (attValue != null) {
            if (parameter) p(name) = attValue
            if (!notSave) obj(name) = attValue
            // if (name.toLowerCase.equalsIgnoreCase("total")) total = attValue.toInt
          }
          resultString = attValue
        }
      } catch {
        case e: Throwable => logError("index maybe error", e)
      }
    }
    if (value != null) resultString = value
    if (filterRegex != null && resultString != null)
      resultString = Util.regexExtract(resultString, filterRegex, filterGroup).asInstanceOf[String].trim
    if (ifExist != null && default != null) {
      if (resultString == null || resultString.isEmpty) resultString = default
      else resultString = ifExist
    }
    else if (ifExist == null && default != null) resultString = default
   if(replace !=null){
          if(replace.contains("->")){
         val rS = replace.split("->")
         if(resultString != null)
         resultString = resultString.replaceAll(rS(0).trim, rS(1).trim)
          }
        }
     if (needCache128) {
          if (resultString.length <= 128) contextCache(name) = resultString
          else logWarning("the length of your context cache content more than 128,we will not save it")
        }else if(needCache) contextCache(name) = resultString

    if (parameter) p(name) = resultString
    if (!notSave) obj(name) = resultString
    resultString
  }
}
