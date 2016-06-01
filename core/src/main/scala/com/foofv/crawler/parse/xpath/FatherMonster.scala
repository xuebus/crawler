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