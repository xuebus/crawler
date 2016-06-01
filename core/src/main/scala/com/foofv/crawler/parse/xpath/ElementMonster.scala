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

/**
 * @author soledede
 */
private[crawler] trait ElementMonster {

  def search(element: Element): String

  def searchList(element: Element): Seq[String]

  def search(element: Element,soldier: String): String = null

  def searchList(element: Element,soldier: String,attr: String): Seq[String] = null
}