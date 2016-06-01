package com.foofv.crawler.zookeeper.zkscheme

import akka.serialization.Serialization
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.leaderelection.LeadershipStatus
import com.foofv.crawler.parse.scheme.nodes.DocumentNode
import com.foofv.crawler.serializer.{Serializer, JavaSerializer}
import com.foofv.crawler.util.Logging
import com.foofv.crawler.zookeeper.ZKCuratorUtil
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{NodeCache, NodeCacheListener}
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException.NodeExistsException

/**
 * Created by soledede on 2015/9/1.
 */
class ZkScheme(conf: CrawlerConf, serialization: Serializer) extends Logging with NodeCacheListener {


  val SCHEME_DIR = conf.get("crawler.zookeeper.config.scheme.dir", "/crawler") + "/config/scheme"

  var nodeCache: NodeCache = null
  private var zk: CuratorFramework = _

  try {
    zk = ZKCuratorUtil.newClient(conf)

    ZKCuratorUtil.mkdir(zk, SCHEME_DIR)
    logInfo("Starting ZooKeeper ....")
    zk = ZKCuratorUtil.newClient(conf)
    nodeCache = new NodeCache(zk, SCHEME_DIR)
    nodeCache.start()
    nodeCache.getListenable.addListener(this)
  }
  catch {
    case e: Exception => logError("init zk_scheme faield!", e)
  }


  override def nodeChanged(): Unit = {
    println("changed!")

  }


  def addSchemeTree(path: String, value: AnyRef): Unit = {
    try {
      zk.create().withMode(CreateMode.PERSISTENT).forPath(path, serialization.newInstance().serializeArray(value))
    }
    catch {
      case e: NodeExistsException => {
        try {
          zk.delete().forPath(path)
          zk.create().withMode(CreateMode.PERSISTENT).forPath(path, serialization.newInstance().serializeArray(value))
        }
        catch {
          case e: Exception => logError("scheme try add faield!", e)
        }
      }
    }
  }

  def readObje[T](filename: String): Option[T] = {
    val fileData = zk.getData().forPath(SCHEME_DIR + "/" + filename)
    try {
      Some(serialization.newInstance().deserialize(fileData))
    } catch {
      case e: Exception => {
        logWarning("Exception while reading persisted file, deleting", e)
        zk.delete().forPath(SCHEME_DIR + "/" + filename)
        None
      }
    }
  }

  def close(): Unit = {
    nodeCache.close()
    zk.close()
  }
}


object ZkScheme {

  def main(args: Array[String]) {
    val SCHEME_DIR = "/crawler/config/scheme"
    val s = new ZkScheme(new CrawlerConf(), Serializer("java", new CrawlerConf()))
    s.addSchemeTree(SCHEME_DIR + "/" + "testscheme", new DocumentNode)
    println(s.readObje("testscheme"))

  }
}
