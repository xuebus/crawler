package com.foofv.crawler.queue

import com.foofv.crawler.CrawlerConf

object TestKafka {
  
  def main(args: Array[String]): Unit = {
    KafkaMessageQueue(new CrawlerConf).sendMsg("一切的一切")
  }

}