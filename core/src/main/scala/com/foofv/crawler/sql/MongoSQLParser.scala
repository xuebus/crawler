package com.foofv.crawler.sql

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.storage.MongoStorage

import scala.collection.JavaConverters._
import scala.collection.mutable.Map

/**
 * Created by soledede on 2015/9/28.
 */
class MongoSQLParser private(conf: CrawlerConf) extends SQLParser {
  override def parse(sql: String): Seq[Map[String, AnyRef]] = {

   val r= MongoStorage.getDocumentList(sql)
    if(r == null) return null
    val docs =r.asScala
    val seq = docs.toSeq.map(_.asScala)
    seq
  }
}

object MongoSQLParser {
  var s: MongoSQLParser = null

  def apply(conf: CrawlerConf): SQLParser = {
    if (s == null) s = new MongoSQLParser(conf)
    s
  }
}

object MongoTestSqlParser{
  def main(args: Array[String]) {
    println(MongoSQLParser(new CrawlerConf()).parse("select shopId from dianping_city_region_merchant_href"))
   // println(MongoSQLParser(new CrawlerConf()).parse("select _id,tag from sys_soledede"))

  }
}
