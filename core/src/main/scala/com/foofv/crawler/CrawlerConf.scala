package com.foofv.crawler

import com.foofv.crawler.conf.impl.DefaultConfiguration
import com.foofv.crawler.downloader.Downloader
import com.foofv.crawler.local.{DataEntityPersistLocal, FetchLocal, ParseProcessLocal}
import com.foofv.crawler.schedule.{ITaskQueue, TaskManager}
import com.foofv.crawler.schedule.manager.{LoacalTaskManager, RedisTaskManager}

import scala.collection.JavaConverters._
import scala.collection.mutable.{HashMap, LinkedHashSet}
import com.foofv.crawler.util.Logging

/**
  * @author:soledede
  * @email:wengbenjue@163.com
  */
private[crawler] class CrawlerConf(loadDefaults: Boolean) extends Cloneable with Logging with Serializable with DefaultConfiguration {
  private[crawler] val settings = new HashMap[String, String]()
  import CrawlerConf._

  /** Create a CrawlerConf that loads defaults from system properties and the classpath */
  def this() = this(true)


  var taskManager: TaskManager = _
  var taskQueue: ITaskQueue = _
  var redis: ITaskQueue = _


  //local
  var fetchLocal: FetchLocal = _
  var parseProcess: ParseProcessLocal = _
  var dataEntityPersistLocal: DataEntityPersistLocal = _

  init()

  def init() = {
    //submit task to queue for local cache
    if (local) {
      this.set("local", "true")
      this.taskManager = new LoacalTaskManager(this)

      //get entity from queue and start fetch url
      this.fetchLocal = new FetchLocal(this)

      //parser for local
      this.parseProcess = new ParseProcessLocal(this)

      //save data  local
      this.dataEntityPersistLocal = new DataEntityPersistLocal(this)

    } else {
      this.set("local", "false")
      //submit task to queue for distribute env
      this.taskManager = new RedisTaskManager(this)
    }

    this.taskQueue = ITaskQueue("sortSet", this)
    this.redis = ITaskQueue("list", this)


  }



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
