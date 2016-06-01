package com.foofv.crawler.parse.example

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import com.foofv.crawler.parse.TopicParser
import com.foofv.crawler.parse.xpath.HtmlSoldier

class ExampleParser extends TopicParser {

  def tess(soldier: String, att: String, content: String) = {
    HtmlSoldier() % (soldier, att, content)
  }
  def tess(soldier: String, content: String) = {
    HtmlSoldier() % (soldier, content)
  }

  def tess(soldier: String, attrName: String, element: Element) = {
    HtmlSoldier() % (soldier, attrName, element)
  }
}

object ExampleParser {
  def main(args: Array[String]): Unit = {
    val html = Jsoup.connect("http://soledede.com/").get().toString()
    val parser = new ExampleParser();
    //print(new TestParser().tess(".core", html))

    println("============获取Elements===========")
    println(parser.tess("h3", "innerHtml", html))

    println("============获取Elements所对应的 String===========")

    val elist: Seq[Element] = new ExampleParser().tess("h3", html)
    if (elist != null && elist.size > 0)
      for (e <- elist) {
        println(parser.tess("h3", "innerHtml", e))
      }

    println("=====================单个字串定位======================")

    println(parser.tess(".core .col-md-6.core-left .core-grid h3", "innerHtml", html))

  }
}