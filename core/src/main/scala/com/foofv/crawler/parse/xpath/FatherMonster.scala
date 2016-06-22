package com.foofv.crawler.parse.xpath

import org.jsoup.nodes.Element
import org.jsoup.Jsoup

/**
 *  @author soledede
 */
private[crawler] abstract class FatherMonster extends TextMonster with ElementMonster {

   def searchElement(element: Element): Element

   def searchElements(element: Element): Seq[Element]
   
   def searchElement(element: Element,soldier: String): Element = {null}

   def searchElements(element: Element,soldier: String): Seq[Element] ={null}

  override def search(text: String): String = {
    if (text != null) search(Jsoup.parse(text)) else null
  }

  override def searchList(text: String): Seq[String] = {
    if (text != null) searchList(Jsoup.parse(text)) else null
  }

  def searchElement(text: String): Element = {
    if (text != null) searchElement(Jsoup.parse(text)) else null
  }

  def selectElements(text: String): Seq[Element] = {
    if (text != null) searchElements(Jsoup.parse(text)) else null
  }

  def hasAttr(): Boolean

}