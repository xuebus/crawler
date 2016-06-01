package com.foofv.crawler.antispamming

import com.foofv.crawler.schedule.ITaskQueueFactory
import com.foofv.crawler.util.Logging
import com.foofv.crawler.schedule.{ITaskQueue, RedisMapTaskQueueImpl}
import com.foofv.crawler.schedule.RedisMapTaskQueueImpl
import com.foofv.crawler.CrawlerConf
import org.apache.commons.lang3.StringUtils
import com.foofv.crawler.redis.cli.{RedisOps, RedisOpsAkkaImpl, IRedisClientFactory}
import redis.ByteStringSerializer
import redis.ByteStringDeserializer
import com.foofv.crawler.util.Util
import com.foofv.crawler.util.constant.Constant

class RedisAntiSpamming(conf: CrawlerConf) extends AntiSpamming with Logging with IRedisClientFactory {

  //redis client
  private[this] val redis = createRedisClient()

  //generate String key
  private def generateKey(ip: String, domain: String, item: String) = {
    val separator = conf.get("crawler.redis.complexkey.separator", Util.separator).trim()
    if (ip == null) StringUtils.deleteWhitespace(domain + separator + item)
    else
      StringUtils.deleteWhitespace(ip + separator + domain + separator + item)
  }

  //implement IRedisClientFactory.createRedisClient
  def createRedisClient(): RedisOps = {
    RedisOps.createRedis("akka", conf)
  }

  // set value
  private def setValue(ip: String, domain: String, item: String, value: String): Boolean = {
    val key = generateKey(ip, domain, item)
    val r = redis.setValue(key, value)
    r
  }

  private def setValue(domain: String, item: String, value: String): Boolean = {
    setValue(null, domain, item, value)
  }

  // set value with exSeconds
  private def setValue(ip: String, domain: String, item: String, value: String, exSeconds: Long): Boolean = {
    val key = generateKey(ip, domain, item)
    val r = redis.setValue(key, value, exSeconds)
    r
  }

  // get value
  private def getValue[T: ByteStringDeserializer](ip: String, domain: String, item: String): Option[String] = {
    var key = ""
    if (ip == null) key = generateKey(null, domain, item)
    else
      key = generateKey(ip, domain, item)
    val r = redis.getValue[String](key)
    //TODO Test
    //redis.delKey(Seq(key))
    r
  }

  private def getValue[T: ByteStringDeserializer](domain: String, item: String): Option[String] = {
    getValue(null, domain, item)
  }

  def changeRefusedState(ip: String, domain: String, isRefused: Boolean): Boolean = {
    changeRefusedState(ip, domain, isRefused, Constant(conf).ANTI_INTERVSL)
    //setValue(ip, domain, RedisAntiSpamming.Key_isRefused, isRefused.toString())
  }

  def changeRefusedState(ip: String, domain: String, isRefused: Boolean, exSeconds: Long): Boolean = {
    setValue(ip, domain, RedisAntiSpamming.Key_isRefused, isRefused.toString(), exSeconds)
  }

  def isRefused(ip: String, domain: String): Boolean = {
    val tmpValue = getValue[String](ip, domain, RedisAntiSpamming.Key_isRefused)
    if (tmpValue != null && !tmpValue.toString.trim.equalsIgnoreCase("")) tmpValue.getOrElse("false").toBoolean
    else false
  }

  def changeIsReSearchIp(ip: String, domain: String, isReSearch: Boolean): Boolean = {
    setValue(ip, domain, RedisAntiSpamming.Key_isReSearch, isReSearch.toString())
  }

  def changeForbiddenValue(domain: String, value: (Boolean, Int, Long, Long)): Boolean = {
    setValue(domain, RedisAntiSpamming.key_fobidden_value, translate(value._1, value._2, value._3, value._4))
  }

  def forbiddenValue(domain: String): (Boolean, Int, Long, Long) = {
    getValue[String](domain, RedisAntiSpamming.key_fobidden_value) match {
      case Some(forBiddenValue) =>
        val array = forBiddenValue.split(Util.separator)
        //val fobiddenStatus = if(array[0]) true else false
        (array(0).toBoolean, array(1).toInt, array(2).toLong, array(3).toLong)
      case None => null
    }
  }

  def isReSearchIp(ip: String, domain: String): Boolean = {
    getValue(ip, domain, RedisAntiSpamming.Key_isReSearch).getOrElse("true").toBoolean
  }

  def changeRefuseIntervalFetch(ip: String, domain: String, refuseIntervalFetch: Short): Boolean = {
    setValue(ip, domain, RedisAntiSpamming.Key_refuseIntervalFetch, refuseIntervalFetch.toString())
  }

  def refuseIntervalFetch(ip: String, domain: String): Short = {
    getValue[String](ip, domain, RedisAntiSpamming.Key_refuseIntervalFetch).getOrElse("-1").toShort
  }

  def changeOpenAverageRate(ip: String, domain: String, openAverageRate: Boolean): Boolean = {
    setValue(ip, domain, RedisAntiSpamming.Key_openAverageRate, openAverageRate.toString())
  }

  def openAverageRate(ip: String, domain: String): Boolean = {
    getValue[String](ip, domain, RedisAntiSpamming.Key_openAverageRate).getOrElse("false").toBoolean
  }

  def changeAverageTime(ip: String, domain: String, averageTime: Long): Boolean = {
    setValue(ip, domain, RedisAntiSpamming.Key_averageTime, "" + averageTime)
  }

  def averageTime(ip: String, domain: String): Long = {
    getValue(ip, domain, RedisAntiSpamming.Key_averageTime).getOrElse("-1").toLong
  }

  def isRefusedMoudle(taskId: String, domain: String, ip: String, costTime: Long, statusCode: Int, refusedState: Boolean, content: String = ""): Boolean = {
    domain match {
      case "waimai.meituan.com" =>
        val m = Util.regexExtract(content, "(429)+", 1)
        if (m != null && !m.asInstanceOf[String].trim().equalsIgnoreCase("")) true else false
      case _ => true
    }
  }

  private def translate(forbiddenStatus: Boolean, requestCount: Int, forbiddenInterval: Long, normalInterval: Long): String = {
    forbiddenStatus.toString() + Util.separator + requestCount.toString() + Util.separator + forbiddenInterval.toString() + Util.separator + normalInterval.toString()
  }
}

object RedisAntiSpamming {
  val Key_isRefused = "isRefused"
  val Key_isReSearch = "isReSearch"
  val Key_refuseIntervalFetch = "refuseIntervalFetch"
  val Key_openAverageRate = "openAverageRate"
  val Key_averageTime = "averageTime"
  val key_fobidden_value = "forbiddenValue"
}

object Test extends Logging {

  var ip = "8.8.8.8"
  var domain = "www.google.com"

  def main(args: Array[String]): Unit = {
    //val test = new RedisAntiSpamming(new CrawlerConf)

    /* logInfo("-----------testBefore--------------")
    testBefore(test)
    logInfo("-----------testBefore--------------")
    testing(test)
    logInfo("-----------testAfter--------------")
    testAfter(test)*/
    //testR
    println("jjk".toBoolean)
  }

  def testR() = {
    println(Util.regexExtract("sdfsdf428999kjs425f0d", "(429)+", 1))
  }

  def testBefore(test: AntiSpamming) {
    val r1 = test.isRefused(ip, domain)
    logInfo("r1--" + r1)
    val r2 = test.isReSearchIp(ip, domain)
    logInfo("r2--" + r2)
    val r3 = test.refuseIntervalFetch(ip, domain)
    logInfo("r3--" + r3)
    val r4 = test.openAverageRate(ip, domain)
    logInfo("r4--" + r4)
    val r5 = test.averageTime(ip, domain)
    logInfo("r5--" + r5)
  }

  def testing(test: AntiSpamming) {
    val r1 = test.changeRefusedState(ip, domain, true)
    logInfo("r1--" + r1)
    val r2 = test.changeIsReSearchIp(ip, domain, true)
    logInfo("r2--" + r2)
    val r3 = test.changeRefuseIntervalFetch(ip, domain, 100)
    logInfo("r3--" + r3)
    val r4 = test.changeOpenAverageRate(ip, domain, true)
    logInfo("r4--" + r4)
    val r5 = test.changeAverageTime(ip, domain, 99999999)
    logInfo("r5--" + r5)
  }

  def testAfter(test: AntiSpamming) {
    testBefore(test)
  }
}


