package com.foofv.crawler.parse.xpath

import org.jsoup.nodes.Element

/**
 * @author soledede
 */
private[crawler] trait ElementMonster {

  def search(element: Element): String

  def searchList(element: Element): Seq[String]

  def search(element: Element,soldier: String): String = null

  def searchList(element: Element,soldier: String,attr: String): Seq[String] = null
}