package com.foofv.crawler.parse.xpath

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import scala.collection.JavaConversions._
import org.jsoup.nodes.TextNode
import org.apache.commons.collections.CollectionUtils
import scala.collection.mutable.ListBuffer

/**
 * @author soledede
 */
private[crawler] class CssMonster(classeText: String, attrName: String) extends FatherMonster {

  def this(classeText: String) = this(classeText, null)

  def this() = this(null, null)

  def value(element: Element): String = {
    if (attrName == null) {
      element.outerHtml()
    } else if ("innerHtml".equalsIgnoreCase(attrName)) {
      //println("CssMonster"+element.html())
      element.html()
    } else if ("text".equalsIgnoreCase(attrName)) {
      text(element)
    } else if ("allText".equalsIgnoreCase(attrName)) {
      element.text
    } else {
      element.attr(attrName)
    }
  }

   def value(element: Element,soldier: String): String = {
    if (soldier == null) {
      element.outerHtml()
    } else if ("innerHtml".equalsIgnoreCase(soldier)) { 
      //println("CssMonster"+element.html())
      element.html()
    } else if ("text".equalsIgnoreCase(soldier)) {
      text(element)
    } else if ("allText".equalsIgnoreCase(soldier)) {
      element.text
    } else {
      element.attr(soldier)
    }
  }
  
  def text(element: Element): String = {
    val textBuilder = new StringBuilder()
    //if( element.isInstanceOf[TextNode]) textBuilder.append(element.asInstanceOf[TextNode].text())
    for (n: Node <- element.childNodes() if n.isInstanceOf[TextNode]) {
      textBuilder.append(n.asInstanceOf[TextNode].text())
    }
    textBuilder.toString()
  }

  override def search(element: Element): String = {
    val els = searchElements(element)
    if (!CollectionUtils.isEmpty(els))
      return value(els.get(0))
    return null
  }

  override def searchList(element: Element): Seq[String] = {
    val valueList = new ListBuffer[String]()
    val elements = searchElements(element)
    if (CollectionUtils.isNotEmpty(elements)) {
      for (e <- elements) {
        val v = value(e);
        if (v != null) {
          valueList.add(v);
        }
      }
    }
    valueList
  }

  override def search(element: Element, soldier: String): String = {
     val els = searchElements(element,soldier)
    if (!CollectionUtils.isEmpty(els))
      return value(els.get(0),soldier)
    return null
  }

  override def searchList(element: Element, soldier: String,attr: String): Seq[String] = {
     val valueList = new ListBuffer[String]()
    val elements = searchElements(element,soldier)
    if (CollectionUtils.isNotEmpty(elements)) {
      for (e <- elements) {
        val v = value(e,attr);
        if (v != null) {
          valueList.add(v);
        }
      }
    }
    valueList
  }

  override def searchElement(element: Element): Element = {
    val els = element.select(classeText)
    if (CollectionUtils.isNotEmpty(els))
      return els.get(0)
    return null
  }

  override def searchElements(element: Element): Seq[Element] = element.select(classeText)

  override def searchElement(element: Element, soldier: String): Element = {

    val els = element.select(soldier)
    if (CollectionUtils.isNotEmpty(els))
      return els.get(0)
    return null
  }

  override def searchElements(element: Element, soldier: String): Seq[Element] =  element.select(soldier)

  override def hasAttr(): Boolean = { attrName != null }
}