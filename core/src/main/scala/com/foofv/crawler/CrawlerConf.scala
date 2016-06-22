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
 **/

package com.foofv.crawler

import scala.collection.JavaConverters._
import scala.collection.mutable.{HashMap, LinkedHashSet}
import com.foofv.crawler.util.Logging

/**
 * @author:soledede
 * @email:wengbenjue@163.com
 */
class CrawlerConf(loadDefaults: Boolean) extends Cloneable with Logging with Serializable {

  import CrawlerConf._

  /** Create a CrawlerConf that loads defaults from system properties and the classpath */
  def this() = this(true)

  private[crawler] val settings = new HashMap[String, String]()

  if (loadDefaults) {
    // Load any crawler.* system properties
    for ((k, v) <- System.getProperties.asScala if k.startsWith("crawler.")) {
      settings(k) = v
    }
  }

  def set(key: String, value: String): CrawlerConf = {
    if (key == null) {
      throw new NullPointerException("null key")
    }
    if (value == null) {
      throw new NullPointerException("null value")
    }
    settings(key) = value
    this
  }


  /** Set multiple parameters together */
  def setAll(settings: Traversable[(String, String)]) = {
    this.settings ++= settings
    this
  }

  /** Set a parameter if it isn't already configured */
  def setIfMissing(key: String, value: String): CrawlerConf = {
    if (!settings.contains(key)) {
      settings(key) = value
    }
    this
  }


  /** Remove a parameter from the configuration */
  def remove(key: String): CrawlerConf = {
    settings.remove(key)
    this
  }

  /** Get a parameter; throws a NoSuchElementException if it's not set */
  def get(key: String): String = {
    settings.getOrElse(key, throw new NoSuchElementException(key))
  }

  /** Get a parameter, falling back to a default if not set */
  def get(key: String, defaultValue: String): String = {
    settings.getOrElse(key, defaultValue)
  }

  /** Get a parameter as an Option */
  def getOption(key: String): Option[String] = {
    settings.get(key)
  }

  /** Get all parameters as a list of pairs */
  def getAll: Array[(String, String)] = settings.clone().toArray

  /** Get a parameter as an integer, falling back to a default if not set */
  def getInt(key: String, defaultValue: Int): Int = {
    getOption(key).map(_.toInt).getOrElse(defaultValue)
  }

  /** Get a parameter as a long, falling back to a default if not set */
  def getLong(key: String, defaultValue: Long): Long = {
    getOption(key).map(_.toLong).getOrElse(defaultValue)
  }

  /** Get a parameter as a double, falling back to a default if not set */
  def getDouble(key: String, defaultValue: Double): Double = {
    getOption(key).map(_.toDouble).getOrElse(defaultValue)
  }

  /** Get a parameter as a boolean, falling back to a default if not set */
  def getBoolean(key: String, defaultValue: Boolean): Boolean = {
    getOption(key).map(_.toBoolean).getOrElse(defaultValue)
  }

  /** Get all akka conf variables set on this CrawlerConf */
  def getAkkaConf: Seq[(String, String)] =
  /* This is currently undocumented. If we want to make this public we should consider
   * nesting options under the crawler namespace to avoid conflicts with user akka options.
   * Otherwise users configuring their own akka code via system properties could mess up
   * crawler's akka options.
   *
   *   E.g. crawler.akka.option.x.y.x = "value"
   */
    getAll.filter { case (k, _) => isAkkaConf(k) }


  /** Does the configuration contain a given parameter? */
  def contains(key: String): Boolean = settings.contains(key)

  /** Copy this object */
  override def clone: CrawlerConf = {
    new CrawlerConf(false).setAll(settings)
  }

  /**
   * By using this instead of System.getenv(), environment variables can be mocked
   * in unit tests.
   */
  private[crawler] def getenv(name: String): String = System.getenv(name)


  /**
   * Return a string listing all keys and values, one per line. This is useful to print the
   * configuration out for debugging.
   */
  def toDebugString: String = {
    settings.toArray.sorted.map { case (k, v) => k + "=" + v }.mkString("\n")
  }
}

private[crawler] object CrawlerConf {

  def isAkkaConf(name: String): Boolean = name.startsWith("akka.")


}
