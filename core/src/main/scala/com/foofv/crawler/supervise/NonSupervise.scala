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
package com.foofv.crawler.supervise

import com.foofv.crawler.parse.worker.WorkerInf
import com.foofv.crawler.util.Logging

/**
 * @author soledede
 */
private[crawler]
class NonSupervise extends Supervise with Logging {

  override def addWorker(worker: WorkerInf) {}
  
  override def delWorker(worker: WorkerInf) {}
  
  override def redaPersistedObj(): Seq[WorkerInf] = (Nil)
  
  override def close() {}
  
}