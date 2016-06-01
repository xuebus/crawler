package com.foofv.crawler.resource.master

import com.foofv.crawler.rsmanager.Res
import java.util.concurrent.PriorityBlockingQueue
import com.foofv.crawler.parse.worker.WorkerInf

private[crawler] class ResourceAllocator(priorityRes: PriorityBlockingQueue[Res]) {

  var resHashMap: collection.mutable.HashMap[String, Double] = new collection.mutable.HashMap[String, Double]()
  var resList = resHashMap.toList.sortBy(_._2).reverse

  def size = {
    refreshResList
    resList.size
  }

  def take(master: Master): String = {
    if (size > 0) {
      println(size + "take come...." + resHashMap)
      var temp = resList(0)

      for (res <- resHashMap) {
        if (!master.idToWorker.contains(res._1)) {
          resHashMap -= res._1
        }
      }
      refreshResList
      if (size > 0) {
        temp = resList(0)
      } else temp = null
      //resList = resList.drop(0)
      // resList = resList
      if (temp == null) null else temp._1
    } else {
      println(size + "take else come....")
      null
    }
  }

  def refreshResList = {
    resList = resHashMap.toList.sortBy(_._2).reverse
  }

  //get worker id acording available resource
  def availableWorkerId(master: Master): String = {
    var workerId: String = null
    /* if (priorityRes.size() > 0) {
      val res = priorityRes.poll()
      workerId = res.workerId
      resHashMap(workerId) = res.total
      println(priorityRes.size() + "availableWorkerId....")
    }*/
    take(master)
    /*workerId = take
    workerId*/
  }
}