package com.foofv.crawler.parse.worker

/**
 * the state of worker for parse
 * @author soledede
 */
object WorkerState extends Enumeration{
  
   type WorkerState = Value

  val ALIVE, DEAD, DECOMMISSIONED, UNKNOWN = Value

}