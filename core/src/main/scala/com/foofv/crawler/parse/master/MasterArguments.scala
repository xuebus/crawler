package com.foofv.crawler.parse.master

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Util
import com.foofv.crawler.util.IntExtract

/**
 * 
 * Parse the parameters 
 * @author soledede
 */
private[crawler]
class MasterArguments(args: Array[String],conf: CrawlerConf) {
  var host = Util.localHostName()
  var port = 9999
  var propertiesFile: String = null
  
  
   // Check for settings in environment variables
  if (System.getenv("CRAWLER_PARSER_MASTER_HOST") != null) {
    host = System.getenv("CRAWLER_PARSER_MASTER_HOST")
  }
  if (System.getenv("CRAWLER_PARSER_MASTER_PORT") != null) {
    port = System.getenv("CRAWLER_PARSER_MASTER_PORT").toInt
  }

  parse(args.toList)

  // This mutates the SparkConf, so all accesses to it must be made after this line
  propertiesFile = Util.loadDefaultProperties(conf, propertiesFile)


  def parse(args: List[String]): Unit = args match {
    case ("--ip" | "-i") :: value :: tail =>
      Util.checkHost(value, "ip no longer supported, please use hostname " + value)
      host = value
      parse(tail)

    case ("--host" | "-h") :: value :: tail =>
      Util.checkHost(value, "Please use hostname " + value)
      host = value
      parse(tail)

    case ("--port" | "-p") :: IntExtract(value) :: tail =>
      port = value
      parse(tail)

    case ("--properties-file") :: value :: tail =>
      propertiesFile = value
      parse(tail)

    case ("--help") :: tail =>
      printUsageAndExit(0)

    case Nil => {}

    case _ =>
      printUsageAndExit(1)
  }

  /**
   * Print usage 
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Master [options]\n" +
      "\n" +
      "Options:\n" +
      "  -i HOST, --ip HOST     Hostname to listen on (deprecated, please use --host or -h) \n" +
      "  -h HOST, --host HOST   Hostname to listen on\n" +
      "  -p PORT, --port PORT   Port to listen on (default: 9080)\n" +
      "  --properties-file FILE Path to a custom Crawler properties file.\n" +
      "                         Default is conf/crawler-defaults.conf.")
    System.exit(exitCode)
  }
  
  
  
}