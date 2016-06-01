package com.foofv.crawler.util.constant

import com.foofv.crawler.CrawlerConf

/**
 * record constant,you can change key using CrawlerConfig
 * @author soledede
 */
private[crawler] class Constant private(conf: CrawlerConf) {

  //schedule
  /* val CRAWLER_TASK_SORTEDSET_KEY = conf.get("crawler.task.sortedset.key", "msfc_sortset_task")
   //priority queue for schedule
   val CRAWLER_TASK_LIST_KEY = conf.get("crawler.task.list.key", "msfc_list_task")*/

  //schedule test
  /*  val CRAWLER_TASK_SORTEDSET_KEY = conf.get("crawler.task.sortedset.key", "msfc_sortset_task_test1")
   //priority queue for schedule test
   val CRAWLER_TASK_LIST_KEY = conf.get("crawler.task.list.key", "msfc_list_task_test1")*/

  /*//schedule test
  val CRAWLER_TASK_SORTEDSET_KEY = conf.get("crawler.task.sortedset.key", "msfc_sortset_task_test")
  //priority queue for schedule test
  val CRAWLER_TASK_LIST_KEY = conf.get("crawler.task.list.key", "msfc_list_task_test")*/
  val CRAWLER_TASK_SORTEDSET_KEY = conf.get("crawler.task.sortedset.key", "msfc_sortset_task")
  //priority queue for schedule
  val CRAWLER_TASK_LIST_KEY = conf.get("crawler.task.list.key", "msfc_list_task")

  //
  val CRAWLER_TASK_CONTEXT_DATA = "contextData"
  val CRAWLER_TASK_PARAMS_DATA = "paramsData"

  //kafka
  //val productTopic = conf.get("com.crawler.kafka.producter.topic", "crawler_v1")
  val PRODUCT_TOPIC = conf.get("com.crawler.kafka.producter.topic", "test")
  val CONSUME_TOPIC = conf.get("com.crawler.kafka.consumer.topic", "test")

  /*val PRODUCT_TOPIC = conf.get("com.crawler.kafka.producter.topic", "test1")
  val CONSUME_TOPIC = conf.get("com.crawler.kafka.consumer.topic", "test1")*/

/*  val BROKERS = conf.get("com.crawler.kafka.brokers", "192.168.1.202:9092")
  val KAFKA_ZK = conf.get("com.crawler.kafka.zk", "192.168.1.202:2181")*/

  val BROKERS = conf.get("com.crawler.kafka.brokers", "123.57.239.78:9092")
  val KAFKA_ZK = conf.get("com.crawler.kafka.zk", "123.57.239.78:2181")

  val KAFKA_GROUPID = "group1"

  //redis
  val REDIS_SERVER_IP = "123.57.239.78"

  //val REDIS_SERVER_IP = "192.168.1.203"

  //request parameter
  val PARAMSPATA = "paramsData"

  //regex

  val DOLLAR = "\\$\\{(\\w+)\\}"

  val DOLLAR_GET = "\\$(\\{\\w+\\})"

  val DOLLA_PAGE_GET = "(\\$\\{[a-zA-z0-9]+\\})"

  val USERAGENT_SEPATOR = "#___#"
  val SEPATOR = "_"

  //anti
  val ANTI_INTERVSL: Long = conf.getLong("crawler.anti.interval", 10 * 60 * 60)

  // the interval time to get another one when getting a task from Redis.sortedset 
  val CRAWLER_GET_NEXT_TASK_INTERVAL_KEY = "crawler.get.next.task.from.sortedset.interval"

  val CRAWLER_AGENT_MASTER_SCHEDULE_TASK_DELAY_TIME_IF_TOO_FAST = conf.getInt("crawler.agentmaster.schedule.task.delay.time.if.too.fast", 60 * 1000)

  val CRAWLER_AGENT_MASTER_SCHEDULE_TASK_RETRY_INTERVAL_IF_NO_WORKERS = conf.getLong("crawler.agentmaster.schedule.task.retry.interval.if.no.workers", 30 * 60 * 1000)

  val CRAWLER_CONTROL_TASK_GROUP_SIZE = conf.getInt("crawler.control.task.group.size", 10000)

  val CRAWLER_CONTROL_TASK_BATCH_INTERVAL = conf.getLong("crawler.control.task.batch.interval", 30000)

  val CRAWLER_AGENT_WORKER_PROXY_HANDLE_TASK_COUNT_PER_MINUTE_PROXYHOST = conf.getInt("crawler.agentworker.proxy.handle.task.count.per.minute.proxyhost", 40)

  val CRAWLER_AGENT_WORKER_HANDLE_TASK_COUNT_PER_MINUTE_PROXYHOST = conf.getInt("crawler.agentworker.handle.task.count.per.minute.proxyhost", 20)

  val CRAWLER_AGENT_WOEKER_HANDLE_TASK_DELAY_TIME_IF_TOO_FAST = conf.getInt("crawler.agentworker.handle.task.delay.time.if.too.fast", 5 * 60 * 1000)

  val CRAWLER_DTWORKER_DEFAULT_WORK_INTERVAL = conf.getLong("crawler.dtworker.default.work.interval", 10 * 1000)

  val CRAWLER_AGENT_WORKER_PULL_HTTPPROXY_INTERVAL = conf.getLong("crawler.agentwork.pull.httpproxy.interval", 10 * 1000)

  val CRAWLER_AGENT_WORKER_PULL_USERAGENT_INTERVAL = conf.getLong("crawler.agentwork.pull.useragent.interval", 10 * 60 * 1000)

  // seconds
  val CRAWLER_AGENT_WORKER_SLEEP_TIME_IF_REFUSED = conf.getLong("crawler.agentwork.SLEEP_TIME_IF_REFUSED", 30 * 60)


  //prefix


  //web
  val WEB_STATIC_RESOURCE_DIR = "static"


}

object Constant {
  var constant: Constant = null

  def apply(conf: CrawlerConf): Constant = {
    if (constant == null) constant = new Constant(conf)
    constant
  }

  val CRAWLEr_AGENT_WORKER_HTTP_CONTEXT_PROXY_HOST = "http_proxy_host"
  val CRAWLER_AGENT_WORKER_TASK_NO = "crawler_agent_worker_task_no"

  val CRAWLER_AGENT_WORKER_INVALID_HTTP_PROXY = "invalid_http_proxy"
}