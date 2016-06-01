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
import java.net.URL

/**
 * Interface for Document
 * Operator liking JQuery
 * @author soledede
 */
private[crawler] trait Soldier {

  def xpath(express: String): Soldier

  // $("soledede")
  def $(soldier: String): Soldier

  // %("soledede","http://soledede.com")
  def %(soldier: String, elements: Seq[Element]): Seq[Element] = { null }

  def %(soldier: String, content: String): Seq[Element] = { null }

  def %(soldier: String, element: Element): Seq[Element] = { null }

  def %(soldier: String, attrName: String, element: Element): String = { null }

  def %(soldier: String, url: URL): Seq[Element] = { null }

  def %(soldier: String, attrName: String, elements: Seq[Element]): String = { null }

  def %(soldier: String, attrName: String, content: String): String = { null }

  def %(soldier: String, attrName: String, url: URL): String = { null }

  def %(content: String): Seq[Element] = { null }

  // $("soledede","name")
  def $(soldier: String, attrName: String): Soldier

  def css(soldier: String): Soldier

  def css(soldier: String, attrName: String): Soldier

  def search(monster: TextMonster): Soldier

  def searchList(monster: TextMonster): Soldier

  def links(): Soldier

  def aLinks(): Seq[String]

  def nodes(): Seq[Soldier]

  def texts(): Seq[String]

  def value(): String

  def elements(): Seq[Element] = { null }
  
  def filterNot(elements: Seq[Element],soldier:String):Seq[Element] = {null}
}

