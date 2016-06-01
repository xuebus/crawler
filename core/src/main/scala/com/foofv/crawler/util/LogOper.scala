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
package com.foofv.crawler.util

/**
 * Record log 
 * You can implement by file,database and so on
 * @author soledede
 */
private[crawler]
trait LogOper {
  
  def logTimes(taskId: String,domain: String,ip: String,requestTime: String,responseTIme: String,timeCost: String): Boolean = { true }
  
  def logTimes(taskId: String, domain: String, ip: String, startTime: Long, endTime: Long, timeCost: Long, statusCode: Int, refusedState: Boolean, content: String = ""): Boolean = { true }

}