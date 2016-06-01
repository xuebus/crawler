/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foofv.crawler.antispamming

/**
 * You can do some logistic when your server ip is refused
 * @author soledede
 */
private[crawler] trait AntiSpamming {

  def changeRefusedState(ip: String, domain: String, isRefused: Boolean): Boolean

  def changeRefusedState(ip: String, domain: String, isRefused: Boolean, exSeconds: Long): Boolean

  def isRefused(ip: String, domain: String): Boolean

  def changeForbiddenValue(domain: String, value: (Boolean, Int, Long, Long)): Boolean

  def forbiddenValue(domain: String): (Boolean, Int, Long, Long)

  def changeIsReSearchIp(ip: String, domain: String, isReSearch: Boolean): Boolean

  def isReSearchIp(ip: String, domain: String): Boolean

  def changeRefuseIntervalFetch(ip: String, domain: String, refuseIntervalFetch: Short): Boolean

  def refuseIntervalFetch(ip: String, domain: String): Short

  def changeOpenAverageRate(ip: String, domain: String, openAverageRate: Boolean): Boolean

  def openAverageRate(ip: String, domain: String): Boolean

  def changeAverageTime(ip: String, domain: String, averageTime: Long): Boolean

  def averageTime(ip: String, domain: String): Long

  def isRefusedMoudle(taskId: String, domain: String, ip: String, costTime: Long, statusCode: Int, refusedState: Boolean, content: String = ""): Boolean

}

private[crawler] trait IAntiSpammingFactory {
  def createAntiSpamming: AntiSpamming
}