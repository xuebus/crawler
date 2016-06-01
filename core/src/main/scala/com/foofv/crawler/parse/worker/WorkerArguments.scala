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
package com.foofv.crawler.parse.worker

import com.foofv.crawler.CrawlerConf
import java.lang.management.ManagementFactory
import com.foofv.crawler.util.Util
import com.foofv.crawler.util.IntExtract
import com.foofv.crawler.util.MemoryParam

private[crawler] class WorkerArguments(args: Array[String], conf: CrawlerConf) {
  var host = Util.localHostName()
  var port = 0
  var cores = inferDefaultCores()
  var memory = inferDefaultMemory()
  var masters: Array[String] = null
  var propertiesFile: String = null
  var controlUrl: String = null

  // Check for settings in environment variables
  if (System.getenv("CRAWLER_PARSE_WORKER_PORT") != null) {
    port = System.getenv("CRAWLER_PARSE_WORKER_PORT").toInt
  }
  if (System.getenv("CRAWLER_PARSE_WORKER_CORES") != null) {
    cores = System.getenv("CRAWLER_PARSE_WORKER_CORES").toInt
  }
  if (conf.getenv("CRAWLER_PARSE_WORKER_MEMORY") != null) {
    memory = Util.memoryStringToMb(conf.getenv("CRAWLER_PARSE_WORKER_MEMORY"))
  }

  parse(args.toList)

  // This mutates the CrawlerConf, so all accesses to it must be made after this line
  propertiesFile = Util.loadDefaultProperties(conf, propertiesFile)

  checkWorkerMemory()

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

    case ("--cores" | "-c") :: IntExtract(value) :: tail =>
      cores = value
      parse(tail)

    case ("--memory" | "-m") :: MemoryParam(value) :: tail =>
      memory = value
      parse(tail)

    case ("--properties-file") :: value :: tail =>
      propertiesFile = value
      parse(tail)

    case ("--control" | "-u") :: value :: tail =>
      controlUrl = value
      parse(tail)

    case ("--help") :: tail =>
      printUsageAndExit(0)

    case value :: tail =>
      if (masters != null) {
        printUsageAndExit(1)
      }
      masters = value.stripPrefix("crawler://").split(",").map("crawler://" + _)
      parse(tail)

    case Nil =>
      if (masters == null) {
        printUsageAndExit(1)
      }

    case _ =>
      printUsageAndExit(1)
  }

  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Worker [options] <master>\n" +
        "\n" +
        "Master must be a URL of the form crawler://hostname:port\n" +
        "\n" +
        "Options:\n" +
        "  -c CORES, --cores CORES  Number of cores to use\n" +
        "  -m MEM, --memory MEM     Amount of memory to use (e.g. 1000M, 2G)\n" +
        "  -i HOST, --ip IP         Hostname to listen on (deprecated, please use --host or -h)\n" +
        "  -h HOST, --host HOST     Hostname to listen on\n" +
        "  -p PORT, --port PORT     Port to listen on (default: random)\n" +
        "  -u control://hostname:port, --control control://hostname:port\n" +
        "  --properties-file FILE   Path to a custom crawler properties file.\n" +
        "                           Default is conf/crawler-defaults.conf.")
    System.exit(exitCode)
  }

  def inferDefaultCores(): Int = {
    Runtime.getRuntime.availableProcessors()
  }

  def inferDefaultMemory(): Int = {
    val ibmVendor = System.getProperty("java.vendor").contains("IBM")
    var totalMb = 0
    try {
      val bean = ManagementFactory.getOperatingSystemMXBean()
      if (ibmVendor) {
        val beanClass = Class.forName("com.ibm.lang.management.OperatingSystemMXBean")
        val method = beanClass.getDeclaredMethod("getTotalPhysicalMemory")
        totalMb = (method.invoke(bean).asInstanceOf[Long] / 1024 / 1024).toInt
      } else {
        val beanClass = Class.forName("com.sun.management.OperatingSystemMXBean")
        val method = beanClass.getDeclaredMethod("getTotalPhysicalMemorySize")
        totalMb = (method.invoke(bean).asInstanceOf[Long] / 1024 / 1024).toInt
      }
    } catch {
      case e: Exception => {
        totalMb = 2 * 1024
        System.out.println("Failed to get total physical memory. Using " + totalMb + " MB")
      }
    }
    // Leave out 1 GB for the operating system, but don't return a negative memory size
    math.max(totalMb - 1024, 512)
  }

  def checkWorkerMemory(): Unit = {
    if (memory <= 0) {
      val message = "Memory can't be 0, missing a M or G on the end of the memory specification?"
      throw new IllegalStateException(message)
    }
  }

}