/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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