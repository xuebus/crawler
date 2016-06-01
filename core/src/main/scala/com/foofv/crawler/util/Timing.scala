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
 * Take task from distributed queue periodically
 */
private[crawler]
class Timing(clock: TimerClock, var period: Long, callback:() => Unit, name: String)
  extends Logging {

  private var thread = new Thread("RecurringTimer - " + name) {
    setDaemon(true)
    override def run() { circle }
  }

  @volatile private var prevTime = -1L
  @volatile private var nextTime = -1L
  @volatile private var stopped = false


  def getStartTime(): Long = {
    (math.floor(clock.currentTime.toDouble / period) + 1).toLong * period
  }

 
  def getRestartTime(originalStartTime: Long): Long = {
    val gap = clock.currentTime - originalStartTime
    (math.floor(gap.toDouble / period).toLong + 1) * period + originalStartTime
  }

  def start(startTime: Long): Long = synchronized {
    nextTime = startTime
    thread.start()
    logInfo("Started timer for " + name)
    nextTime
  }

 
  def start(): Long = {
    start(getStartTime())
  }

 
  def stop(interruptTimer: Boolean): Long = synchronized {
    if (!stopped) {
      stopped = true
      if (interruptTimer) {
        thread.interrupt()
      }
      thread.join()
      logInfo("Stopped timer for " + name + " after time " + prevTime)
    }
    prevTime
  }
  
  def resetNextTime(nextTime: Long){
    this.nextTime = nextTime
    logInfo("reset timer for " + name + " next time " + com.foofv.crawler.util.Util.convertDateFormat(nextTime))
  }
  
  def restart() {
    stop(true)
    thread = null
    thread = new Thread("RecurringTimer - " + name) {
      setDaemon(true)
      override def run() { circle }
    }
    start()
    stopped = false
  }

  /**
   * Timing call the callback every interval.
   */
  private def circle() {
    try {
      while (!stopped) {
        clock.waitToTime(nextTime)
        callback()
        prevTime = nextTime
        nextTime += period
        logDebug("Callback for " + name + " called at time " + com.foofv.crawler.util.Util.convertDateFormat(prevTime))
      }
    } catch {
      case e: InterruptedException =>
    }
  }
}

private[crawler]
object Timing {

  def main(args: Array[String]) {
    var lastRecurTime = 0L
    val period = 1000

    def onRecur(time: Long) {
      val currentTime = System.currentTimeMillis()
      println("" + currentTime + ": " + (currentTime - lastRecurTime))
      lastRecurTime = currentTime
    }
    val timer = new  Timing(new SystemTimerClock(), period, test, "Test")
    timer.start()
    Thread.sleep(30 * 1000)
    timer.stop(true)
  }
  
    def test():Unit ={
    println("进来了，当前时间："+System.currentTimeMillis())
    
  }
}

