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

import scala.collection.mutable.ListBuffer

/**
 * @author soledede
 */
private[crawler] abstract class FatherSoldier extends Soldier {

  override def css(soldier: String): Soldier = {
    $(soldier)
  }

  override def css(soldier: String, attrName: String): Soldier = {
    $(soldier, attrName)
  }

  def search(monster: TextMonster): Soldier = search(monster, texts)

  def searchList(monster: TextMonster): Soldier = searchList(monster, texts)

  protected def searchList(monster: TextMonster, texts: Seq[String]): Soldier = {
    var list = new ListBuffer[String]()
    for (text <- texts if text != null) {
      list ++ monster.searchList(text)
    }
    new TextSoldier(list)
  }

  protected def search(monster: TextMonster, texts: Seq[String]): Soldier = {
    var list = new ListBuffer[String]()
    for (text <- texts if text != null) {
      list + monster.search(text)
    }
    new TextSoldier(list)
  }

}