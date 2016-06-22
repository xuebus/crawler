package com.foofv.crawler.enumeration

/**
 * The type of Task
 * @author soledede
 */
object CrawlerTaskType  extends Enumeration{
  type Type = Value 
  val THEME_CRAWLER, GENERAL_CRAWLER, SPECIFIC_CRAWLER =Value
}