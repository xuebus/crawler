
/**
Copyright [2015] [soledede]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
  * */
package com.foofv.crawler.zookeeper

import scala.collection.JavaConversions._
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.KeeperException
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Logging

/**
 * @author:soledede
 * @email:wengbenjue@163.com
 */
object ZKCuratorUtil extends Logging {

  val ZK_CONNECTION_TIMEOUT_MILLIS = 15000
  val ZK_SESSION_TIMEOUT_MILLIS = 60000
  val RETRY_WAIT_MILLIS = 5000
  val MAX_RECONNECT_ATTEMPTS = 3

  def newClient(conf: CrawlerConf): CuratorFramework = {
    val ZK_URL = conf.get("crawler.deploy.zookeeper.url", "123.57.239.78:2181")
    val zk = CuratorFrameworkFactory.newClient(ZK_URL,
      ZK_SESSION_TIMEOUT_MILLIS, ZK_CONNECTION_TIMEOUT_MILLIS,
      new ExponentialBackoffRetry(RETRY_WAIT_MILLIS, MAX_RECONNECT_ATTEMPTS))
    zk.start()
    zk
  }

  def mkdir(zk: CuratorFramework, path: String) {
    if (zk.checkExists().forPath(path) == null) {
      try {
        zk.create().creatingParentsIfNeeded().forPath(path)
      } catch {
        case nodeExist: KeeperException.NodeExistsException =>
        // do nothing, ignore node existing exception.
        case e: Exception => throw e
      }
    }
  }

  def deleteRecursive(zk: CuratorFramework, path: String) {
    if (zk.checkExists().forPath(path) != null) {
      for (child <- zk.getChildren.forPath(path)) {
        zk.delete().forPath(path + "/" + child)
      }
      zk.delete().forPath(path)
    }
  }
}
