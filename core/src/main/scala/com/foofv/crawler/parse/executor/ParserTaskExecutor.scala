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