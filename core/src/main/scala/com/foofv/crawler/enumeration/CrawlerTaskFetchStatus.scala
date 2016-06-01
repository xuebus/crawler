package com.foofv.crawler.enumeration


object CrawlerTaskFetchStatus extends Enumeration {
  type Status = Value 
  val FETCHING = Value("fetching") 
  val FETCHED =Value("fetched")
}

object NeedSaveParser extends Enumeration {
  type Door = Value
  val NEED, NO_NEED =Value
}

object StoragePlugin extends Enumeration {
  type Inf = Value
  val HBASE = Value("hbase")
  val QINIUYUN = Value("qiniucloud")
  val OTHER =Value("other")
}