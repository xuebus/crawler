/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foofv.crawler.parse.xpath

import java.util.Collections
import java.util.ListIterator
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import com.foofv.crawler.CrawlerConf
import java.net.URL

/**
 * @author soledede
 */
private[crawler] class HtmlSoldier private (var doc: Document, var text: String, elements: Seq[Element], conf: CrawlerConf) extends FatherSoldier {

  def this() = this(null, null, null, null)

  def this(text: String) = this(null, text, null, null)

  def this(doc: Document) = this(doc, null, null, null)

  def this(elements: Seq[Element]) = this(null, null, elements, null)

  def this(text: String, conf: CrawlerConf) = this(null, text, null, conf)

  def this(doc: Document, conf: CrawlerConf) = this(doc, null, null, conf)

  def this(elements: Seq[Element], conf: CrawlerConf) = this(null, null, elements, conf)

  init

  private def init = {
    if (conf != null) {
      if (conf.get("crawler.conf.disable_html_entity_escape", "true").toBoolean && !conf.get("crawler.conf.disable_html_entity_escape_inited", "false").toBoolean) {
        Entities.EscapeMode.base.getMap().clear();
        Entities.EscapeMode.extended.getMap().clear();
        conf.set("crawler.conf.disable_html_entity_escape_inited", "true")
      } else {
        Entities.EscapeMode.base.getMap().clear();
        Entities.EscapeMode.extended.getMap().clear();
      }
    }
    if (this.doc == null && this.text != null) { this.doc = Jsoup.parse(text) }
  }

  override def links(): Soldier = xpath("//a/@href")

  override def xpath(express: String): Soldier = {
    searchList(TextMonster.xpath(express))
  }

  override def search(monster: TextMonster): Soldier = searchList(monster, texts)

  override def searchList(monster: TextMonster): Soldier = {
    if (monster.isInstanceOf[FatherMonster]) searchElements(monster.asInstanceOf[FatherMonster]) else searchList(monster, texts)
  }

  override def texts(): Seq[String] = {
    val l = new ListBuffer[String]()
    for (e: Element <- getElements) {
      l ++ e.toString()
    }
    l
  }

  def getElements(): Seq[Element] = {
    if (elements == null) {
      return Collections.singletonList[Element](this.doc)
    }
    elements
  }

  private def searchElements(fatherMonster: FatherMonster): Soldier = {
    val e = getElements.listIterator()
    if (fatherMonster.hasAttr()) {
      val listString = new ListBuffer[String]()
      while (e.hasNext()) {
        val element = checkElement(e)
        val listElement = fatherMonster.searchList(element)
        listString.addAll(listElement)
      }
      return new TextSoldier(listString)
    } else {
      val listElements = new ListBuffer[Element]()
      while (e.hasNext()) {
        val element = checkElement(e)
        listElements.addAll(fatherMonster.searchElements(element))
      }
      return new HtmlSoldier(listElements)
    }
  }

  private def searchElements(soldier: String, fatherMonster: FatherMonster, elements: Seq[Element]): Seq[Element] = {
    val e = elements.listIterator()
    val listElements = new ListBuffer[Element]()
    while (e.hasNext()) {
      val element = checkElement(e)
      val list = fatherMonster.searchElements(element, soldier)
      listElements.addAll(list)
    }
    listElements
  }

  private def searchTexts(soldier: String, fatherMonster: FatherMonster, elements: Seq[Element], attr: String): String = {
    val e = elements.listIterator()
    val texts = new StringBuilder()
    if (attr == null) return null
    while (e.hasNext()) {
      val element = checkElement(e)
      val listElement = fatherMonster.searchList(element, soldier, attr)
      listElement.map(texts.append(_))
    }
    texts.toString()
  }

  def checkElement(elementIterator: ListIterator[Element]): Element = {
    val element = elementIterator.next();
    if (element != null && !(element.isInstanceOf[Document])) {
      val root: Document = new Document(element.ownerDocument().baseUri());
      val clone = element.clone();
      root.appendChild(clone);
      //elementIterator.set(root);
      return root
    }
    return element
  }

  override def $(soldier: String): Soldier = {
    searchElements(TextMonster.$(soldier))
  }

  override def $(soldier: String, attrName: String): Soldier = {
    searchElements(TextMonster.$(soldier, attrName))
  }

  override def %(soldier: String, elements: Seq[Element]): Seq[Element] = { searchElements(soldier, TextMonster.%(), elements) }

  override def %(soldier: String, content: String): Seq[Element] = { %(soldier, Collections.singletonList[Element](Jsoup.parse(content))) }

  override def %(soldier: String, attrName: String, elements: Seq[Element]): String = { searchTexts(soldier, TextMonster.%(), elements, attrName) }

  override def %(soldier: String, attrName: String, content: String): String = { %(soldier, attrName, Collections.singletonList[Element](Jsoup.parse(content))) }

  override def %(soldier: String, element: Element): Seq[Element] = { %(soldier, List(element)) }

  override def %(soldier: String, attrName: String, element: Element): String = { %(soldier, attrName, List(element)) }

  override def %(content: String): Seq[Element] = { Collections.singletonList[Element](Jsoup.parse(content)) }

  override def filterNot(elements: Seq[Element], soldier: String): Seq[Element] = {
    val subElements = %(soldier, elements)
    (elements.toSet &~ subElements.toSet).toList
  }

  override def nodes(): Seq[Soldier] = {
    val soldiers = new ListBuffer[Soldier]()
    for (e: Element <- getElements()) {
      val childElements = new ListBuffer[Element]();
      childElements.add(e);
      soldiers.add(new HtmlSoldier(childElements));
    }
    return soldiers;
  }

  override def value(): String = {
    val buildString = new StringBuilder()
    for (t <- texts) {
      buildString.append(t)
    }
    buildString.toString()
  }

  override def aLinks(): Seq[String] = {
    val linkList = new ListBuffer[String]()
    for (e: Element <- getElements()) {
      val link = e.select("a")
      if (link != null && !link.isEmpty()) {
        linkList.add(link.attr("abs:href"))
      }
    }
    linkList
  }
}

object HtmlSoldier {

  var extractObj: HtmlSoldier = null

  def apply(): HtmlSoldier = {
    if (extractObj == null) extractObj = new HtmlSoldier()
    extractObj
  }

}