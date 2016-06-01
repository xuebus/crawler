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

import akka.actor.Actor
import org.slf4j.Logger

/**
 * @author soledede
 * @email wengbenjue@163.com
 * A trait to enable logging all Akka actor messages. Here's an example of using this:
 *
 */
private[crawler] trait ActorLogReceive {
  self: Actor =>

  override def receive: Actor.Receive = new Actor.Receive {

    private val _receiveRecordLog = receiveRecordLog

    override def isDefinedAt(o: Any): Boolean = _receiveRecordLog.isDefinedAt(o)

    override def apply(o: Any): Unit = {
      if (log.isDebugEnabled) {
        log.debug(s"[actor] received message $o from ${self.sender}")
      }
      val start = System.nanoTime
      _receiveRecordLog.apply(o)
      val timeTaken = (System.nanoTime - start).toDouble / 1000000
      if (log.isDebugEnabled) {
        log.debug(s"[actor] handled message ($timeTaken ms) $o from ${self.sender}")
      }
    }
  }

  def receiveRecordLog: Actor.Receive

  protected def log: Logger
}
