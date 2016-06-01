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
package com.foofv.crawler

import com.foofv.crawler.util.TimerClock
import com.foofv.crawler.util.SystemTimerClock
import com.foofv.crawler.util.Timing
import com.foofv.crawler.util.Logging
import com.foofv.crawler.schedule.ITaskQueue
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.schedule.RedisSortedSetTaskQueueImpl
import com.foofv.crawler.blockqueue.CrawlerArrayBlockQueueImpl

/**
 * Pull task to local blocking queue
 * @author soledede
 */
private[crawler]
class PullTaskToBlockQueue[T](conf: CrawlerConf,taskQueue: ITaskQueue) extends Logging{

  private val clock = new SystemTimerClock()
  
  private val pullInterval = conf.getLong("crawler.pullInterval", 1000) 
  
  private val pullIntervalTiming = new Timing(clock,pullInterval,intevalPull,"PullTaskToBlockQueue")
  
  def start() = {
    pullIntervalTiming.start()
  }
  
  def intevalPull():Unit ={
    logDebug("进来了，当前时间： " + +System.currentTimeMillis())
    //CrawlerArrayBlockQueueImpl.put(taskQueue.getFirstElement("zdum").asInstanceOf[CrawlerTaskEntity])
   // println(CrawlerArrayBlockQueueImpl.size())
  }
  
  def stop():Boolean = {
    true
  }
  
}

object testL {
  
  def main(args: Array[String]): Unit = {
    val conf = new CrawlerConf
    new PullTaskToBlockQueue[CrawlerTaskEntity](new CrawlerConf(),ITaskQueue("sortSet",conf)).start()
  }
}