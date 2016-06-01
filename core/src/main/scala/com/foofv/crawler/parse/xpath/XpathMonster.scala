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

import us.codecraft.xsoup.XPathEvaluator
import us.codecraft.xsoup.Xsoup
import org.jsoup.nodes.Element
import org.jsoup.Jsoup
import scala.collection.JavaConversions._
import org.apache.commons.collections.CollectionUtils

/**
 * @author soledede
 */
private[crawler] class XpathMonster(xpathText: String) extends FatherMonster {

  private var xPathEvaluator: XPathEvaluator = Xsoup.compile(xpathText)

  override def search(element: Element): String = xPathEvaluator.evaluate(element).get

  override def searchList(element: Element): Seq[String] = xPathEvaluator.evaluate(element).list()

  override def searchElement(element: Element): Element = {
    val elements = searchElements(element)
    if (CollectionUtils.isNotEmpty(elements)) elements.get(0) else null
  }

  override def searchElements(element: Element): Seq[Element] = xPathEvaluator.evaluate(element).getElements()

  override def hasAttr(): Boolean = xPathEvaluator.hasAttribute()
}