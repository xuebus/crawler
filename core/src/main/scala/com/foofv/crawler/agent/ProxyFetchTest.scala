package com.foofv.crawler.agent

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * Created by soledede on 2015/8/26.
 */
object ProxyFetchTest {

  def main(args: Array[String]) {
    for (j <- 1 to 10) {
      val doc = Jsoup.connect(s"http://www.kuaidaili.com/proxylist/${j}/")
      /*.data("query", "Java")
      .userAgent("Mozilla")
      .cookie("auth", "token")
      .timeout(3000)
      .post();*/
      val els: Elements = doc.get().body().select("div#container div#list table.table.table-bordered.table-striped tbody tr")
      for (i <- 0 to els.size() - 1) {
        val tds = els.get(i).select("td")
        val ip = tds.get(0).text()
        val port = tds.get(1).text()
        println("(\""+ip + "\"," + port+"),")

      }
    }
  }

}
