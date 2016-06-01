package com.foofv.crawler.parse

import com.foofv.crawler.parse.xpath.HtmlSoldier
import com.foofv.crawler.util.Logging
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.CrawlerConf

class URLAnalyzerImpl(conf: CrawlerConf) extends HtmlSoldier with Analyzer with Logging {

  override def extractUrls(resObj: ResObj): Seq[String] = {
    //TODO
    null
  }

}

object URLAnalyzerImpl{
  var urlAnalyzer: URLAnalyzerImpl = null
  def apply(conf: CrawlerConf): URLAnalyzerImpl = {
    if(urlAnalyzer == null) urlAnalyzer = new URLAnalyzerImpl(conf)
    urlAnalyzer
  }
}