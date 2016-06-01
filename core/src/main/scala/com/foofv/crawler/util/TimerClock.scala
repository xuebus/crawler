/**
 *Copyright [2015] [soledede]
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
**/

package com.foofv.crawler.util

/**
 * Interface to Clock
 * @author soledede
 */
private[crawler]
trait TimerClock {
  def currentTime(): Long
  def waitToTime(targetTime: Long): Long
}

private[crawler]
class SystemTimerClock() extends TimerClock with Logging{

  val minSleepime = 25L

  def currentTime(): Long = {
    System.currentTimeMillis()
  }

  def waitToTime(targetTime: Long): Long = {
    var currentTime = 0L
    currentTime = System.currentTimeMillis()

    var waitTime = targetTime - currentTime
    if (waitTime <= 0) {
      return currentTime
    }

    val howSleepTime = math.max(waitTime / 10.0, minSleepime).toLong

    while (true) {
      currentTime = System.currentTimeMillis()
      waitTime = targetTime - currentTime
      if (waitTime <= 0) {
        return currentTime
      }
      val sleepTime = math.min(waitTime, howSleepTime)
      Thread.sleep(sleepTime)
    }
    -1
  }
}

