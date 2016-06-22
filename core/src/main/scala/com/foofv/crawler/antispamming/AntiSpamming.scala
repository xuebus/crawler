package com.foofv.crawler.antispamming

/**
 * You can do some logistic when your server ip is refused
 * @author soledede
 */
private[crawler] trait AntiSpamming {

  def changeRefusedState(ip: String, domain: String, isRefused: Boolean): Boolean

  def changeRefusedState(ip: String, domain: String, isRefused: Boolean, exSeconds: Long): Boolean

  def isRefused(ip: String, domain: String): Boolean

  def changeForbiddenValue(domain: String, value: (Boolean, Int, Long, Long)): Boolean

  def forbiddenValue(domain: String): (Boolean, Int, Long, Long)

  def changeIsReSearchIp(ip: String, domain: String, isReSearch: Boolean): Boolean

  def isReSearchIp(ip: String, domain: String): Boolean

  def changeRefuseIntervalFetch(ip: String, domain: String, refuseIntervalFetch: Short): Boolean

  def refuseIntervalFetch(ip: String, domain: String): Short

  def changeOpenAverageRate(ip: String, domain: String, openAverageRate: Boolean): Boolean

  def openAverageRate(ip: String, domain: String): Boolean

  def changeAverageTime(ip: String, domain: String, averageTime: Long): Boolean

  def averageTime(ip: String, domain: String): Long

  def isRefusedMoudle(taskId: String, domain: String, ip: String, costTime: Long, statusCode: Int, refusedState: Boolean, content: String = ""): Boolean

}

private[crawler] trait IAntiSpammingFactory {
  def createAntiSpamming: AntiSpamming
}