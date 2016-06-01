/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foofv.crawler.parse

import org.jsoup.nodes.Document
import com.foofv.crawler.parse.util.Link
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.CrawlerConf

/**
 * liking parse url to jsoup document
 * @author soledede
 */
private[crawler] trait Analyzer {

  def generateDocment(url: String): Document = { null }

  def extractLinks(): Seq[Link] = { null }

  def extractUrls(resObj: ResObj): Seq[String] = { null }

  def extractContent(): AnyRef = { null }

}

private[crawler] trait AnalyzerFactory {
  def createAnalyzer(): Analyzer
}

object Analyzer {

  def apply(analyzerType: String, conf: CrawlerConf) = {
    analyzerType match {
      case "url" => URLAnalyzerImpl(conf)
    }
  }

}