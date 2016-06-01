package com.foofv.crawler.util.listener

/**
 * Created by soledede on 2015/9/17.
 */
sealed trait TraceListenerEvent

case class JobStarted(jobId: String, jobName: String, seedNum: Int) extends TraceListenerEvent

case class JobTaskFailed(jobId: String, jobName: String, num: Int) extends TraceListenerEvent

case class JobTaskCompleted(jobId: String, jobName: String, num: Int) extends TraceListenerEvent

case class JobTaskAdded(jobId: String, jobName: String, num: Int) extends TraceListenerEvent

case class Keys(parttern: String) extends TraceListenerEvent


