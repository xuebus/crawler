package com.foofv.crawler.parse

/**
 * Different domain , we need different strategy to parse
 * Our concrete DomainParseRule can be instantiated by the container when the application is firing
 * @author soledede
 */
private[crawler]
trait ParserByDomain {

  def parse()
}