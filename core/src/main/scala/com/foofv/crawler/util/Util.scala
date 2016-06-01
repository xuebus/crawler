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
package com.foofv.crawler.util

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.BindException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Locale
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.regex.{Matcher, Pattern}

import com.google.common.net.InetAddresses

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.JavaConversions.propertiesAsScalaMap
import scala.collection.Map
import scala.collection.mutable.ListBuffer
import scala.util.control.ControlThrowable
import scala.util.control.NonFatal
import org.apache.commons.lang3.SystemUtils
import org.eclipse.jetty.util.MultiException
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.CrawlerEnv
import com.foofv.crawler.CrawlerException
import com.google.common.util.concurrent.ThreadFactoryBuilder
import sun.management._
import com.sun.management.OperatingSystemMXBean
import java.text.SimpleDateFormat

import scala.util.matching.Regex

private[crawler] object Util extends Logging {

  val isWindows = SystemUtils.IS_OS_WINDOWS

  private var customHostname: Option[String] = None

  /**
   * Attempt to start a service on the given port, or fail after a number of attempts.
   * Each subsequent attempt uses 1 + the port used in the previous attempt (unless the port is 0).
   *
   * @param startPort The initial port to start the service on.
   * @param maxRetries Maximum number of retries to attempt.
   *                   A value of 3 means attempting ports n, n+1, n+2, and n+3, for example.
   * @param startService Function to start service on a given port.
   *                     This is expected to throw java.net.BindException on port collision.
   */
  def startServiceOnPort[T](
                             startPort: Int,
                             startService: Int => (T, Int),
                             serviceName: String = "",
                             maxRetries: Int = portMaxRetries): (T, Int) = {
    val serviceString = if (serviceName.isEmpty) "" else s" '$serviceName'"
    for (offset <- 0 to maxRetries) {
      // Do not increment port if startPort is 0, which is treated as a special port
      val tryPort = if (startPort == 0) {
        startPort
      } else {
        // If the new port wraps around, do not try a privilege port
        ((startPort + offset - 1024) % (65536 - 1024)) + 1024
      }
      try {
        val (service, port) = startService(tryPort)
        logInfo(s"Successfully started service$serviceString on port $port.")
        return (service, port)
      } catch {
        case e: Exception if isBindCollision(e) =>
          if (offset >= maxRetries) {
            val exceptionMessage =
              s"${e.getMessage}: Service$serviceString failed after $maxRetries retries!"
            val exception = new BindException(exceptionMessage)
            // restore original stack trace
            exception.setStackTrace(e.getStackTrace)
            throw exception
          }
          logWarning(s"Service$serviceString could not bind on port $tryPort. " +
            s"Attempting port ${tryPort + 1}.")
      }
    }
    // Should never happen
    throw new CrawlerException(s"Failed to start service$serviceString on port $startPort")
  }

  def getFormattedClassName(obj: AnyRef): String = {
    obj.getClass.getSimpleName.replace("$", "")
  }

  val portMaxRetries: Int = {
    if (sys.props.contains("crawler.testing")) {
      // Set a higher number of retries for tests...
      sys.props.get("crawler.port.maxRetries").map(_.toInt).getOrElse(100)
    } else {
      Option(CrawlerEnv.get)
        .flatMap(_.conf.getOption("crawler.port.maxRetries"))
        .map(_.toInt)
        .getOrElse(16)
    }
  }

  def separator: String = "_"

  def isBindCollision(exception: Throwable): Boolean = {
    exception match {
      case e: BindException =>
        if (e.getMessage != null) {
          return true
        }
        isBindCollision(e.getCause)
      case e: MultiException => e.getThrowables.exists(isBindCollision)
      case e: Exception => isBindCollision(e.getCause)
      case _ => false
    }
  }


  def localHostNameForURI(): String = {
    customHostname.getOrElse(InetAddresses.toUriString(localIpAddressNew))
  }


  private def findLocalInetAddress(): InetAddress = {
    val defaultIpOverride = System.getenv("Crawler_LOCAL_IP")
    if (defaultIpOverride != null) {
      InetAddress.getByName(defaultIpOverride)
    } else {
      val address = InetAddress.getLocalHost
      if (address.isLoopbackAddress) {
        // Address resolves to something like 127.0.1.1, which happens on Debian; try to find
        // a better address using the local network interfaces
        // getNetworkInterfaces returns ifs in reverse order compared to ifconfig output order
        // on unix-like system. On windows, it returns in index order.
        // It's more proper to pick ip address following system output order.
        val activeNetworkIFs = NetworkInterface.getNetworkInterfaces.toList
        val reOrderedNetworkIFs = if (isWindows) activeNetworkIFs else activeNetworkIFs.reverse

        for (ni <- reOrderedNetworkIFs) {
          val addresses = ni.getInetAddresses.toList
            .filterNot(addr => addr.isLinkLocalAddress || addr.isLoopbackAddress)
          if (addresses.nonEmpty) {
            val addr = addresses.find(_.isInstanceOf[Inet4Address]).getOrElse(addresses.head)
            // because of Inet6Address.toHostName may add interface at the end if it knows about it
            val strippedAddress = InetAddress.getByAddress(addr.getAddress)
            // We've found an address that looks reasonable!
            logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
              " a loopback address: " + address.getHostAddress + "; using " +
              strippedAddress.getHostAddress + " instead (on interface " + ni.getName + ")")
            logWarning("Set Crawler_LOCAL_IP if you need to bind to another address")
            return strippedAddress
          }
        }
        logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
          " a loopback address: " + address.getHostAddress + ", but we couldn't find any" +
          " external IP address!")
        logWarning("Set Crawler_LOCAL_IP if you need to bind to another address")
      }
      address
    }
  }

  def getClassLoader: ClassLoader = getClass.getClassLoader

  /**
   * Get the Context ClassLoader on this thread
   *
   * This should be used whenever passing a ClassLoader to Class.ForName or finding the currently
   * active loader when setting up ClassLoader delegation chains.
   */
  def getContextClassLoader =
    Option(Thread.currentThread().getContextClassLoader).getOrElse(getClass.getClassLoader)

  def tryOrIOException(block: => Unit) {
    try {
      block
    } catch {
      case e: IOException => throw e
      case NonFatal(t) => throw new IOException(t)
    }
  }

  private val daemonThreadFactoryBuilder: ThreadFactoryBuilder =
    new ThreadFactoryBuilder().setDaemon(true)

  /**
   * Create a thread factory that names threads with a prefix and also sets the threads to daemon.
   */
  def namedThreadFactory(prefix: String): ThreadFactory = {
    daemonThreadFactoryBuilder.setNameFormat(prefix + "-%d").build()
  }

  /**
   * Wrapper over newCachedThreadPool. Thread names are formatted as prefix-ID, where ID is a
   * unique, sequentially assigned integer.
   */
  def newDaemonCachedThreadPool(prefix: String): ThreadPoolExecutor = {
    val threadFactory = namedThreadFactory(prefix)
    Executors.newCachedThreadPool(threadFactory).asInstanceOf[ThreadPoolExecutor]
  }

  /**
   * Wrapper over newFixedThreadPool. Thread names are formatted as prefix-ID, where ID is a
   * unique, sequentially assigned integer.
   */
  def newDaemonFixedThreadPool(nThreads: Int, prefix: String): ThreadPoolExecutor = {
    val threadFactory = namedThreadFactory(prefix)
    Executors.newFixedThreadPool(nThreads, threadFactory).asInstanceOf[ThreadPoolExecutor]
  }

  def getAddressHostName(address: String): String = {
    InetAddress.getByName(address).getHostName
  }

  def checkHost(host: String, message: String = "") {
    assert(host.indexOf(':') == -1, message)
  }

  def checkHostPort(hostPort: String, message: String = "") {
    assert(hostPort.indexOf(':') != -1, message)
  }

  /**
   * Convert a quantity in bytes to a human-readable string
   */
  def bytesToString(size: Long): String = {
    val TB = 1L << 40
    val GB = 1L << 30
    val MB = 1L << 20
    val KB = 1L << 10

    val (value, unit) = {
      if (size >= 2 * TB) {
        (size.asInstanceOf[Double] / TB, "TB")
      } else if (size >= 2 * GB) {
        (size.asInstanceOf[Double] / GB, "GB")
      } else if (size >= 2 * MB) {
        (size.asInstanceOf[Double] / MB, "MB")
      } else if (size >= 2 * KB) {
        (size.asInstanceOf[Double] / KB, "KB")
      } else {
        (size.asInstanceOf[Double], "B")
      }
    }
    "%.1f %s".formatLocal(Locale.US, value, unit)
  }

  def megabytesToString(megabytes: Long): String = {
    bytesToString(megabytes * 1024L * 1024L)
  }

  /**
   * Get the local machine's hostname.
   */
  def localHostName(): String = {
    customHostname.getOrElse(localIpAddressHostname)
  }

  lazy val localIpAddress: String = findLocalIpAddress()

  private lazy val localIpAddressNew: InetAddress = findLocalInetAddress()

  lazy val localIpAddressHostname: String = getAddressHostName(localIpAddress)

  private def findLocalIpAddress(): String = {
    val defaultIpOverride = System.getenv("CRAWLER_LOCAL_IP")
    if (defaultIpOverride != null) {
      defaultIpOverride
    } else {
      val address = InetAddress.getLocalHost
      if (address.isLoopbackAddress) {
        // Address resolves to something like 127.0.1.1, which happens on Debian; try to find
        // a better address using the local network interfaces
        // getNetworkInterfaces returns ifs in reverse order compared to ifconfig output order
        // on unix-like system. On windows, it returns in index order.
        // It's more proper to pick ip address following system output order.
        val activeNetworkIFs = NetworkInterface.getNetworkInterfaces.toList
        val reOrderedNetworkIFs = if (isWindows) activeNetworkIFs else activeNetworkIFs.reverse
        for (ni <- reOrderedNetworkIFs) {
          for (
            addr <- ni.getInetAddresses if !addr.isLinkLocalAddress &&
            !addr.isLoopbackAddress && addr.isInstanceOf[Inet4Address]
          ) {
            // We've found an address that looks reasonable!
            logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
              " a loopback address: " + address.getHostAddress + "; using " + addr.getHostAddress +
              " instead (on interface " + ni.getName + ")")
            logWarning("Set CRAWLER_LOCAL_IP if you need to bind to another address")
            return addr.getHostAddress
          }
        }
        logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
          " a loopback address: " + address.getHostAddress + ", but we couldn't find any" +
          " external IP address!")
        logWarning("Set CRAWLER_LOCAL_IP if you need to bind to another address")
      }
      address.getHostAddress
    }
  }

  def loadDefaultProperties(conf: CrawlerConf, filePath: String = null): String = {
    val path = Option(filePath).getOrElse(getDefaultPropertiesFile())
    Option(path).foreach { confFile =>
      getPropertiesFromFile(confFile).filter {
        case (k, v) =>
          k.startsWith("crawler.")
      }.foreach {
        case (k, v) =>
          conf.setIfMissing(k, v)
          sys.props.getOrElseUpdate(k, v)
      }
    }
    path
  }

  def getDefaultPropertiesFile(env: Map[String, String] = sys.env): String = {
    env.get("CRAWLER_CONF_DIR")
      .orElse(env.get("CRAWLER_HOME").map { t => s"$t${File.separator}conf" })
      .map { t => new File(s"$t${File.separator}crawler-defaults.conf") }
      .filter(_.isFile)
      .map(_.getAbsolutePath)
      .orNull
  }

  def getPropertiesFromFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"Properties file $file does not exist")
    require(file.isFile(), s"Properties file $file is not a normal file")

    val inReader = new InputStreamReader(new FileInputStream(file), "UTF-8")
    try {
      val properties = new Properties()
      properties.load(inReader)
      properties.stringPropertyNames().map(k => (k, properties(k).trim)).toMap
    } catch {
      case e: IOException =>
        throw new CrawlerException(s"Failed when loading Crawler properties from $filename", e)
    } finally {
      inReader.close()
    }
  }

  def inShutdown(): Boolean = {
    try {
      val hook = new Thread {
        override def run() {}
      }
      Runtime.getRuntime.addShutdownHook(hook)
      Runtime.getRuntime.removeShutdownHook(hook)
    } catch {
      case ise: IllegalStateException => return true
    }
    false
  }


  def tryOrExit(block: => Unit) {
    try {
      block
    } catch {
      case e: ControlThrowable => throw e
      case t: Throwable => CrawlerUncauthtExceptionHandler.uncaughtException(t)
    }
  }

  /**
   * Convert a Java memory parameter passed to -Xmx (such as 300m or 1g) to a number of megabytes.
   */
  def memoryStringToMb(str: String): Int = {
    val lower = str.toLowerCase
    if (lower.endsWith("k")) {
      (lower.substring(0, lower.length - 1).toLong / 1024).toInt
    } else if (lower.endsWith("m")) {
      lower.substring(0, lower.length - 1).toInt
    } else if (lower.endsWith("g")) {
      lower.substring(0, lower.length - 1).toInt * 1024
    } else if (lower.endsWith("t")) {
      lower.substring(0, lower.length - 1).toInt * 1024 * 1024
    } else {
      // no suffix, so it's just a number in bytes
      (lower.toLong / 1024 / 1024).toInt
    }
  }

  def convertDateFormat(time: Long, format: String = "yyyy-MM-dd HH:mm:ss SSS"): String = {
    val date = new java.util.Date(time)
    val formatter = new SimpleDateFormat(format)
    formatter.format(date)
  }

  def inferCores(): Int = {
    Runtime.getRuntime.availableProcessors()
  }


  def main(args: Array[String]): Unit = {
    /*println("操作系统："+System.getProperty("os.name"))
    val osmxbean = ManagementFactory.getOperatingSystemMXBean().asInstanceOf[OperatingSystemMXBean]
    println("总的物理内存："+osmxbean)
    println(Runtime.getRuntime.maxMemory())*/
  }


  // get HTTP Response Content from ResObj.Response
  def getResponseContent(response: com.foofv.crawler.downloader.Response): String = {

    import org.apache.commons.lang3.StringUtils

    val contentType = response.getContentType
    // get document charset form HTTP Response Header[ContentType], default 'utf-8'
    var charset: String = "utf-8"
    if (StringUtils.isNotBlank(contentType)) {
      var temp = contentType.trim().toLowerCase()
      if (temp.contains("charset")) {
        val charsetStr = temp.split(";").find { x => x.contains("charset") }
        temp = charsetStr.getOrElse(charset)
        temp = StringUtils.deleteWhitespace(temp)
        charset = temp.replace("charset=", "")
      }
    }
    StringUtils.toEncodedString(response.getContent, java.nio.charset.Charset.forName(charset))
  }

  def find(input: String, regex: String): Boolean = {
    if (regex == null || input == null) {
      return false
    }
    Pattern.compile(regex).matcher(input).find()
  }

  def regexExtract(input: String, regex: String): AnyRef = regexExtract(input, regex, -2)

  def regexExtract(input: String, regex: String, group: Int): AnyRef = {
    if (regex == null) {
      return input
    }
    val p: Pattern = Pattern.compile(regex)
    val m: Matcher = p.matcher(input)
    if (group == -2) {
      val list = new ListBuffer[String]

      while (m.find) {
        var i: Int = 1
        while (i <= m.groupCount) {
          {
            list.append(m.group(i))
          }
          i += 1
        }
      }
      return list
    } else {
      if (m.find) {
        if (group == -1) {
          var r = ""
          val s = new StringBuilder
          var i: Int = 0
          while (i <= m.groupCount - 1) {
            {
              s ++= m.group(i)
              if (i != m.groupCount) {
                s ++= " "
              }
            }
            ({
              i += 1
            })
          }
          r =s.toString
          if(r.length==0)
          r = m.group()
          return r
        }
        else {
          return m.group(group)
        }
      }
      else return ""
    }
  }

}

