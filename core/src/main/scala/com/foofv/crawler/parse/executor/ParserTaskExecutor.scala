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
package com.foofv.crawler.parse.executor

import com.foofv.crawler.util.Util
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.TaskRunner


/**
 * Parse tasks by  multithreading, Here we use the thread pool
 * 1.Receive rowkey from Metaq (Schedule by master), then use this rowkey get response and crawler object from hbase
 * 2.We use a BlockingQueue cache the task , and then we take it from BlockingQueue when the thread pool idles
 * 3.Execute it 
 * @author soledede
 */
private[crawler]
class ParserTaskExecutor{
  
  //ParserWorker thread pool
   val threadPool = Util.newDaemonCachedThreadPool("Executor task launch ParserWorker")
   private val runningTasks = new ConcurrentHashMap[Long, ParserTaskRunner]
  
   
   def startParse(){
    
     while(true){
        //get response from blocking queue and put the thread pool if thread pool idles
       val ptr = new ParserTaskRunner()
       this.threadPool.execute(ptr)
     }
   }
}


class ParserTaskRunner extends Runnable{
  
  override def run(){
    
    
  }

}