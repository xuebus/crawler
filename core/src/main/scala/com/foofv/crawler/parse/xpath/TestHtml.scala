package com.foofv.crawler.parse.xpath

import org.jsoup.nodes.Document
import org.jsoup.Jsoup

class TestHtml extends HtmlSoldier{
  this.doc = Jsoup.connect("http://waimai.meituan.com/home/wtw3w7hhv0pc?utm_source=3603").get()
 // print($(".no-result","innerHtml").value)
  //$(".ceiling-search").nodes()
 println( $(".restaurant").links().value())
  //println($(".restaurant").aLinks().toString())
}

object TestHtml{
  def main(args: Array[String]): Unit = {
    new TestHtml()
  }
}