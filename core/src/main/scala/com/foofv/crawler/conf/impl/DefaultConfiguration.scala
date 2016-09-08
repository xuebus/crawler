package com.foofv.crawler.conf.impl

import com.foofv.crawler.conf.Configuration

import scala.util.Try

/**
  * Created by soledede.weng on 2016/6/6.
  */
trait DefaultConfiguration extends Configuration {


  /**
    * check for unique
    */
  lazy val local: Boolean = Try(config.getBoolean("local")).getOrElse(false)
  lazy val fetchThreads : Int = Try(config.getInt("crawler.agent.worker.threadnum")).getOrElse(1)



}
