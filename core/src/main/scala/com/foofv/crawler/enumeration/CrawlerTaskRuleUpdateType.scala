package com.foofv.crawler.enumeration

object CrawlerTaskRuleUpdateType extends Enumeration {
  type Type = Value
  val AUTO_INTELLIGENCE = Value("auto intelligence")
  val FIXED_TIME = Value("fixed time")
  val MACHINE_LEARNING = Value("machine learning")
  
}