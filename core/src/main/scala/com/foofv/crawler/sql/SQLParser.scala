package com.foofv.crawler.sql

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Logging
import scala.collection.mutable.Map

/**
 * Created by soledede on 2015/9/28.
 */
trait SQLParser extends Logging {

  def parse(sql: String): Seq[Map[String, AnyRef]]

}


object SQLParser {

  def apply(sqlType: String, conf: CrawlerConf): SQLParser = {
    sqlType match {
      case "mongo" => MongoSQLParser(conf)
    }
  }
}
