package com.foofv.crawler.conf

import com.typesafe.config.ConfigFactory

/**
  * Created by soledede.weng on 2016/6/2.
  */
trait Configuration {
  /**
    * Application config object.
    */
  val config = ConfigFactory.load()

}
