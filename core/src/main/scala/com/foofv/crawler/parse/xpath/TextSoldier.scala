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

private[crawler] class TextSoldier(text: String, plainTexts: Seq[String]) extends FatherSoldier {

  def this(text: String) = this(text, null)

  def this(plainTexts: Seq[String]) = this(null, plainTexts)

  override def texts(): Seq[String] = {
    if (plainTexts != null) plainTexts else List(text)
  }

  override def value(): String = {
    val buildString = new StringBuilder()
    for (t <- texts) {
      buildString.append(t)
    }
    buildString.toString()
  }

  override def xpath(xpath: String): Soldier = throw new UnsupportedOperationException("unsupported")

  override def $(soldier: String): Soldier = throw new UnsupportedOperationException("unsupported")

  override def $(soldier: String, attrName: String): Soldier = throw new UnsupportedOperationException("unsupported")

  override def css(soldier: String): Soldier = throw new UnsupportedOperationException("unsupported")

  override def css(soldier: String, attrName: String): Soldier = throw new UnsupportedOperationException("unsupported")

  override def search(monster: TextMonster): Soldier = throw new UnsupportedOperationException("unsupported")

  override def searchList(monster: TextMonster): Soldier = throw new UnsupportedOperationException("unsupported")

  override def nodes(): Seq[Soldier] = throw new UnsupportedOperationException("unsupported")

  override def links(): Soldier = throw new UnsupportedOperationException("unsupported")
  
  override def aLinks(): Seq[String] = throw new UnsupportedOperationException("unsupported")
}