package com.foofv.crawler.util.listener

import com.foofv.crawler.util.Logging

/**
 * Created by soledede on 2015/9/17.
 */
trait TraceListener extends Logging{

  def onJobStart(jobstart: JobStarted) = {}

  def onJobTaskFailed(jobTaskFailed: JobTaskFailed): Int = {-1}

  def onJobTaskCompleted(jobTaskCompleted: JobTaskCompleted): Int = {-1}

  def onJobTaskAdded(jobTaskAdded: JobTaskAdded): Int = {-1}

  def onSearch(keys: Keys): Option[Seq[String]] = {null}
}
