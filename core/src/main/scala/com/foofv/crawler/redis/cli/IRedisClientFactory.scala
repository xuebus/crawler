package com.foofv.crawler.redis.cli

import com.foofv.crawler.CrawlerConf

private[crawler]
trait IRedisClientFactory {
  
  def createRedisClient: RedisOps
}
