package com.foofv.crawler.parse.datasource

/**
 * Get data for parsing
 * eg:
 *    We get data from hbase by rowkey
 * @author soledede
 */
private[crawler]
trait ParserDataSource {

  def getParseDataById()
  
}