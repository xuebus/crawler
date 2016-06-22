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
package com.foofv.crawler.rsmanager

import java.util.Random

/**
 * resource info
 * @author soledede
 */
private[crawler] trait ResourseInfo

case class MemoryInfo() extends ResourseInfo {
  var memTotal: Long = _
  var memFree: Long = _
  var swapTotal: Long = _
  var swapFree: Long = _
  var memUsage: Double = _
}

case class CpuInfo(var cpuCores: Int, var cpuUsage: Double) extends ResourseInfo

case class Res(workerId: String, var memUsage: Double, var cpuUsage: Double, var cpuCores: Int, var totalMem: Double) extends Comparable[Res] with ResourseInfo {
  var total: Double = 0

  if (this.memUsage > 0.88) this.memUsage = Math.sqrt(this.memUsage)
  if (this.cpuUsage > 0.88) this.cpuUsage = Math.sqrt(this.cpuUsage)
  val r = new Random()

  total = this.cpuCores * (1 - this.cpuUsage) + this.totalMem * (1 - this.memUsage * Math.log10(this.memUsage + 10)) + r.nextDouble() * 1024 * 312 * r.nextInt(100)

  override def compareTo(o: Res): Int = {
    if ((this.cpuCores * (1 - this.cpuUsage) + this.totalMem * (1 - this.memUsage)) > (o.cpuCores * (1 - o.cpuUsage) + o.totalMem * (1 - o.memUsage))) -1
    else if ((this.cpuCores * (1 - this.cpuUsage) + this.totalMem * (1 - this.memUsage)) < (o.cpuCores * (1 - o.cpuUsage) + o.totalMem * (1 - o.memUsage))) 1
    else 0
  }

  def getWeightTotalRes(): Double = {
    total
  }


}

