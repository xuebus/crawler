package com.foofv.crawler.parse.topic

import java.net.{URL, URLEncoder}
import java.text.SimpleDateFormat
import java.util
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.pattern._
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.control.CrawlerControlImpl
import com.foofv.crawler.deploy.TransferMsg.{DocTrees, JobId}
import com.foofv.crawler.entity.{CrawlerTaskEntity, ResObj}
import com.foofv.crawler.enumeration.HttpRequestMethodType
import com.foofv.crawler.parse.TopicParser
import com.foofv.crawler.parse.scheme.AnalyseSemanticTree
import com.foofv.crawler.parse.scheme.nodes.{DocumentNode, SemanticNode}
import com.foofv.crawler.util.Util
import com.foofv.crawler.util.constant.Constant
import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.apache.http.message.BasicNameValuePair
import org.codehaus.jackson.map.ObjectMapper

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.mutable.{HashMap, ListBuffer, Map}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.Breaks._
import com.foofv.crawler.parse.scheme.ResultParameter

/**
  * Created by soledede on 2015/8/20.
  */
private[crawler] class SchemeTopicParser(conf: CrawlerConf) extends TopicParser {

  override def parse(resObj: ResObj, obj: AnyRef): AnyRef = {
    if (obj != null) {
      if (obj.isInstanceOf[(String, Any)]) {
        val objPersist = obj.asInstanceOf[(String, Any)]
        if (objPersist._2 != null) {
          if (objPersist._2.isInstanceOf[Seq[Map[String, Any]]]) {
            val appendObj = objPersist._2.asInstanceOf[Seq[Map[String, Any]]].map { o =>
              def createDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
              val t = resObj.tastEntity
              o +("currentBatchId" -> t.currentBatchId, "currentDepth" -> t.currentDepth, "jobId" -> t.jobId, "jobName" -> t.jobName, "schemaFile" -> t.schemeFile, "taskType" -> t.taskType.toString, "taskUrl" -> t.taskURI, "taskDomain" -> t.taskDomain, "createDate" -> createDateFormat.format(new Date))
            }
            return (objPersist._1, appendObj)
          }
        }
      }
    }

    val isStream = resObj.tastEntity.isStream

    val content = resObj.response.getContent
    if (isStream == 1 && content != null && content.length > 0) {
      var taleName = resObj.tastEntity.tableName
      if (taleName == null || taleName.isEmpty) taleName = "sys_soledede"
      return (taleName, Map("taskUrl" -> resObj.tastEntity.taskURI, "content" -> content))
    }
    obj
  }

  override def geneNewTaskEntitySet(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {

    var result: (Seq[CrawlerTaskEntity], AnyRef) = null

    try {
      val jobId = resObj.tastEntity.jobId.toString
      var docMap = SchemeTopicParser.schemeDocCacheManager.getIfPresent(jobId)
      if (docMap == null) {
        if (conf.getBoolean("local", false)) {
          val semanticTree = CrawlerControlImpl.semanticTreeMap(jobId)
          SchemeTopicParser.schemeDocCacheManager.put(jobId,semanticTree )
          docMap = SchemeTopicParser.schemeDocCacheManager.getIfPresent(jobId)
        } else {
          if (SchemeTopicParser.controlActor != null) {
            implicit val timeout = akka.util.Timeout.apply(50, java.util.concurrent.TimeUnit.SECONDS)
            val future = SchemeTopicParser.controlActor ? JobId(jobId)
            var r: DocTrees = null
            r = Await.result(future, 50 seconds).asInstanceOf[DocTrees]
            if (future.isCompleted) {
              SchemeTopicParser.schemeDocCacheManager.put(jobId, r.tree)
              docMap = r.tree
            } else {
              logInfo("can't get receive message in 50 seconds!")
            }
            //Thread.sleep(5000)
          } else {
            logError("can't connect controlActor..")
          }
        }
      }
      if (docMap != null) {
        result = walkTree(resObj, docMap.asInstanceOf[util.Map[String, DocumentNode]])
      }
    } catch {
      case e: Exception => logError("parser schemele faield!", e)
    }
    result
  }

  def setCachMap(p: AnyRef, key: String, value: String): AnyRef = {
    var oP: Map[String, AnyRef] = null
    if (p.isInstanceOf[(Map[String, AnyRef], Map[String, AnyRef])]) {
      oP = p.asInstanceOf[(Map[String, AnyRef], Map[String, AnyRef])]._1
    } else if (p.isInstanceOf[Map[String, AnyRef]]) {
      oP = p.asInstanceOf[Map[String, AnyRef]]
    }
    breakable {
      oP.map {
        case (k, v) =>
          val rvalue = Util.regexExtract(v.toString, Constant(conf).DOLLAR, 1).toString
          if (!rvalue.isEmpty) {
            if (key.trim.equalsIgnoreCase(rvalue)) {
              oP(k) = value
              break
            }
          }
      }
    }

    if (p.isInstanceOf[(Map[String, AnyRef], Map[String, AnyRef])]) {
      return (oP, p.asInstanceOf[(Map[String, AnyRef], Map[String, AnyRef])]._2)
    } else if (p.isInstanceOf[Map[String, AnyRef]]) {
      return oP
    }
    null
  }

  private def walkTree(resObj: ResObj, docMap: util.Map[String, DocumentNode]): (Seq[CrawlerTaskEntity], AnyRef) = {

    var taskEntityList = new ListBuffer[CrawlerTaskEntity]()
    var objList: Seq[Map[String, AnyRef]] = null //parsed content list
    var pList: Seq[(Map[String, AnyRef], Map[String, AnyRef])] = null //parsed parameter  list
    var obj: Map[String, AnyRef] = null //parsed content
    var p: Map[String, AnyRef] = null //parsed parameter
    var contextCache: Map[String, AnyRef] = null //parsed contextCache
    var tableName: String = null
    var total = -1
    var totalPageSize = -1
    var paramterHasList = false //parameter has list,as same as isFiled =true in list
    var docId = resObj.tastEntity.schemeDocId

    def cacheFetchVal: Unit = {
      //set context cache
      val resContextCache = resObj.tastEntity.contextJsonString

      if (resContextCache != null && !resContextCache.trim.equalsIgnoreCase("null")) {
        val om: ObjectMapper = new ObjectMapper()
        val rootJsonNode = om.readTree(resContextCache)
        val jode = rootJsonNode.getFields
        while (jode.hasNext()) {
          val key = jode.next().getKey.trim
          val value = rootJsonNode.get(key).getValueAsText
          if (key != null && value != null) {
            if (obj != null)
              obj = setCachMap(obj, key, value).asInstanceOf[Map[String, AnyRef]]
          }
          if (p != null) {
            p = setCachMap(p, key, value).asInstanceOf[Map[String, AnyRef]]
          }
          if (objList != null) {
            objList = for {
              ll <- objList
              sl = setCachMap(ll, key, value).asInstanceOf[Map[String, AnyRef]]
            } yield sl
          }
          if (pList != null) {
            pList = for {
              ll <- pList
              sl = setCachMap(ll, key, value).asInstanceOf[(Map[String, AnyRef], Map[String, AnyRef])]
            } yield sl
          }
        }
      }
    }
    if (docMap.containsKey(docId)) {
      var docNode = docMap.get(docId)
      var nullSmart: Boolean = false
      var execTree = docNode.process(Util.getResponseContent(resObj.response))
      if (execTree == null && docNode.refDocId != null) {
        nullSmart = true
        breakable {
          while (true) {
            val refDocmentId = docNode.refDocId
            if (execTree == null && refDocmentId != null) {
              docNode = docMap.get(refDocmentId)
              execTree = docNode.process(Util.getResponseContent(resObj.response))
            } else break
          }
        }
      }

      if (execTree != null) {
        try {
          val item = execTree.asInstanceOf[(mutable.Map[String, AnyRef], mutable.Map[String, AnyRef], mutable.Map[String, AnyRef], ResultParameter)]
          obj = item._1
          p = item._2
          contextCache = item._3
          val r = item._4
          total = r.total
          totalPageSize = r.totalPageSize
          paramterHasList = r.paramterHasList
        } catch {
          case e: ClassCastException => {
            val itemList = execTree.asInstanceOf[(Seq[mutable.Map[String, AnyRef]], Seq[(mutable.Map[String, AnyRef], mutable.Map[String, AnyRef])], String, ResultParameter)]
            objList = itemList._1
            pList = itemList._2
            val r = itemList._4
            total = r.total
            totalPageSize = r.totalPageSize
            paramterHasList = r.paramterHasList
          }
        }

        /* if (execTree.isInstanceOf[(mutable.Map[String, AnyRef], mutable.Map[String, AnyRef])] && !execTree.isInstanceOf[(Seq[mutable.Map[String, AnyRef]], Seq[mutable.Map[String, AnyRef]])]) {
           //parsed content parameter
           val item = execTree.asInstanceOf[(mutable.Map[String, AnyRef], mutable.Map[String, AnyRef])]
           obj = item._1
           p = item._2
         } else if (execTree.isInstanceOf[(Seq[mutable.Map[String, AnyRef]], Seq[mutable.Map[String, AnyRef]])] && !execTree.isInstanceOf[(mutable.Map[String, AnyRef], mutable.Map[String, AnyRef])]) {
           //parsed content list parameter
           val itemList = execTree.asInstanceOf[(Seq[mutable.Map[String, AnyRef]], Seq[mutable.Map[String, AnyRef]])]
           objList = itemList._1
           pList = itemList._2
         }*/
        if (obj != null && obj.isEmpty) obj = null
        if (p != null && p.isEmpty) p = null
        if (contextCache != null && contextCache.isEmpty) contextCache = null

        if (objList != null && objList.size <= 0) objList = null
        if (pList != null && pList.size <= 0) pList = null

        cacheFetchVal

        /* else if (pList == null && taskItelligent) {
           resObj.tastEntity.schemeDocId = resObj.tastEntity.schemaPreDocId
           resObj.tastEntity.taskURI = resObj.tastEntity.schemaPreUrl
           resObj.tastEntity.schemaRefDocId = docNode.refDocId
           val taskEntity = resObj.tastEntity
           val childTaskEntity = taskEntity.geneChildTaskEntity()
           childTaskEntity.currentDepth -= 1
           taskEntityList += childTaskEntity
           return (taskEntityList, null)
         }*/

        val url = docNode.docUrl
        val docFirstUrl = docNode.docFirstUrl
        val method = docNode.method
        val page = docNode.page
        val pageSize = docNode.pageSize
        val fistPageSize = docNode.firstPagesize
        /*var total = docNode.total
        var totalPageSize = docNode.totalPageSize*/
        var currentRefDocId = docNode.refDocId
        var maxPage = docNode.maxPage
        docId = docNode.docId
        //val taskRefDocId = resObj.tastEntity.schemaRefDocId
        /*  if (taskItelligent && taskRefDocId != null && !"null".equalsIgnoreCase(taskRefDocId)) {
            currentRefDocId = taskRefDocId
            resObj.tastEntity.schemaItelligent = "false"
          }*/
        //val paramterHasList = docNode.paramterHasList //parameter has list,as same as isFiled =true in list
        tableName = docNode.tableName

        //next page
        var refUrl: String = null
        var refDocFirstUrl: String = null
        var refIntelligent = false
        var refMethod: String = "get"
        var refPage: Boolean = false
        var refPageSize: Int = -1
        var refFistPageSize: Int = -1
        val postParamters = docNode.postParameters
        var refPostParamters: mutable.Map[String, AnyRef] = null
        var refMaxPage = -1
        var refStartPage = 1
        if (currentRefDocId != null) {
          //Having child page
          val refDocNode = docMap.get(currentRefDocId)
          if (refDocNode != null) {
            refDocNode.preDocId = docNode.docId
            //TODO  process next page,include page
            refUrl = refDocNode.docUrl
            refDocFirstUrl = refDocNode.docFirstUrl
            refIntelligent = refDocNode.intelligent
            /*if (refIntelligent) {
              resObj.tastEntity.schemaItelligent = "true"
              resObj.tastEntity.schemaPreDocId = docNode.docId
            }*/
            refMethod = refDocNode.method
            refPage = refDocNode.page
            refPageSize = refDocNode.pageSize
            refFistPageSize = refDocNode.firstPagesize
            refPostParamters = refDocNode.postParameters
            refMaxPage = refDocNode.maxPage
            refStartPage = refDocNode.startPage
          }
        }

        if (nullSmart) {
          resObj.tastEntity.currentDepthCompleted = "false"
          var href = resObj.tastEntity.taskURI
          val p = new mutable.HashMap[String, AnyRef]
          val matchList = Util.regexExtract(url, Constant(conf).DOLLAR).asInstanceOf[Seq[String]]
          val urlHostFilter = "http://" + new URL(href).getHost
          href = href.replaceAll(urlHostFilter, "")
          breakable {
            for (m <- matchList) {
              if (!"page".trim.equalsIgnoreCase(m)) {
                p(m) = href
                break
              }
            }
          }
          if (p.isEmpty) p("href") = href
          currentRefDocId = docId
          singleMapParameter(1, docId, currentRefDocId, resObj, taskEntityList, p, url, null, method, page, pageSize, fistPageSize, total, null, totalPageSize, maxPage, contextCache)
        }

        if (refUrl != null) {
          if (pList != null && pList.size > 0) {
            for (p <- pList) {
              singleMapParameter(refStartPage, docId, currentRefDocId, resObj, taskEntityList, p, refUrl, refPostParamters, refMethod, refPage, refPageSize, refFistPageSize, total, refDocFirstUrl, totalPageSize, refMaxPage, contextCache)
            }
          } else if (p != null && !p.isEmpty) {
            if (!paramterHasList) {
              singleMapParameter(refStartPage, docId, currentRefDocId, resObj, taskEntityList, p, refUrl, refPostParamters, refMethod, refPage, refPageSize, refFistPageSize, total, refDocFirstUrl, totalPageSize, refMaxPage, contextCache)
            } else {
              var isList: Boolean = false
              val paList = p.filter(_._2.isInstanceOf[Seq[mutable.Map[String, AnyRef]]]).asInstanceOf[Seq[mutable.Map[String, AnyRef]]]
              if (paList.size <= 0) singleMapParameter(refStartPage, docId, currentRefDocId, resObj, taskEntityList, p, refUrl, refPostParamters, refMethod, refPage, refPageSize, refFistPageSize, total, refDocFirstUrl, totalPageSize, refMaxPage, contextCache)
              else paList.map(_ ++= p.filterNot(_._2.isInstanceOf[Seq[mutable.Map[String, AnyRef]]]).asInstanceOf[mutable.Map[String, AnyRef]])
              for (pa <- paList) {
                singleMapParameter(refStartPage, docId, currentRefDocId, resObj, taskEntityList, pa, refUrl, refPostParamters, refMethod, refPage, refPageSize, refFistPageSize, total, refDocFirstUrl, totalPageSize, refMaxPage, contextCache)
              }
            }
          }

        }
      }
    }

    if (objList != null && objList.size <= 0) objList = null
    if (taskEntityList != null && taskEntityList.size <= 0) taskEntityList = null
    if (objList != null) return (taskEntityList, (tableName, objList))
    else if (obj != null && !obj.isEmpty) {
      if (objList == null) {
        val list = new ListBuffer[Map[String, AnyRef]]
        list += obj
        objList = list
      }
    }
    (taskEntityList, (tableName, objList))
  }

  def setContextMap(contextCache: Map[String, AnyRef], childTaskEntity: CrawlerTaskEntity): Unit = {

    //set context map to entity
    val om: ObjectMapper = new ObjectMapper()
    var context: String = null
    if (contextCache != null) {
      context = om.writeValueAsString(contextCache.asJava)
    }
    if (context != null) childTaskEntity.contextJsonString = context
  }

  private def singleMapParameter(refStartPage: Int, docId: String, currentRefDocId: String, resObj: ResObj, taskEntityList: ListBuffer[CrawlerTaskEntity],
                                 p: AnyRef, refUrl: String, refPostParamters: Map[String, AnyRef],
                                 refMethod: String, page: Boolean, pageSize: Int, fistPageSize: Int, total: Int,
                                 refDocFirstUrl: String, totalPageSize: Int, maxPage: Int, contextCache: Map[String, AnyRef]): Unit = {

    //single page {name:"a",title:"b"}

    val taskEntity = resObj.tastEntity
    val childTaskEntity = taskEntity.geneChildTaskEntity()
    //childTaskEntity.schemaItelligent = intelligent.toString
    childTaskEntity.schemeDocId = currentRefDocId

    var requestP: Map[String, AnyRef] = null
    if (p.isInstanceOf[(Map[String, AnyRef], Map[String, AnyRef])]) {
      val p_c = p.asInstanceOf[(Map[String, AnyRef], Map[String, AnyRef])]
      val rCache = p_c._2
      requestP = p_c._1
      if (rCache != null) setContextMap(rCache, childTaskEntity)
    } else if (p.isInstanceOf[Map[String, AnyRef]]) {
      requestP = p.asInstanceOf[Map[String, AnyRef]]
    }
    if (refMethod.toLowerCase.trim.equals("post")) {
      processPostRequest(refStartPage, taskEntityList, docId, currentRefDocId, taskEntity.currentDepthCompleted.toBoolean, requestP, refPostParamters, childTaskEntity, refUrl, page, pageSize, fistPageSize, total, refDocFirstUrl, totalPageSize, maxPage)
    } else {
      processGetRequest(refStartPage, docId, currentRefDocId, taskEntity.currentDepthCompleted.toBoolean, refUrl, childTaskEntity, taskEntityList, requestP, page, pageSize, fistPageSize, total, refDocFirstUrl, totalPageSize, maxPage)
    }
  }

  private def processGetRequest(refStartPage: Int, docId: String, currentRefDocId: String, currentDepthCompleted: Boolean, refUrl: String, childTaskEntity: CrawlerTaskEntity, taskEntityList: ListBuffer[CrawlerTaskEntity], p: Map[String, AnyRef], page: Boolean, pageSize: Int, fistPageSize: Int, total: Int,
                                refDocFirstUrl: String, totalPageSize: Int, maxPage: Int): Unit = {
    var refu = refUrl
    var refFirstUrl = refDocFirstUrl
    //get request
    childTaskEntity.httpmethod = HttpRequestMethodType.GET

    var regexModel = "(\\$\\{model\\})"
    var regexModelFirst = "(\\$\\{model\\})"

    breakable {
      for (get <- p) {
        var cnt = 0
        val key = get._1
        var valu = get._2.asInstanceOf[String]
        val matchList = Util.regexExtract(refu, Constant(conf).DOLLAR).asInstanceOf[Seq[String]]
        val regexModelUrl = regexModel.replaceAll("model", key)
        breakable {
          for (m <- matchList) {
            if (key.trim.equalsIgnoreCase(m)) {
              refu = refu.replaceAll(regexModelUrl, valu)
              break
            } else cnt += 1
          }
        }

        if (cnt != 0 && cnt == matchList.size)
          refu = childTaskEntity.taskURI

        if (refFirstUrl != null) {
          val matchFirstList = Util.regexExtract(refFirstUrl, Constant(conf).DOLLAR).asInstanceOf[Seq[String]]
          val regexModelFirstUrl = regexModelFirst.replaceAll("model", key)
          breakable {
            for (m <- matchFirstList) {
              if (key.trim.equalsIgnoreCase(m)) {
                refFirstUrl = refFirstUrl.replaceAll(regexModelFirstUrl, valu)
                break
              }
            }
          }
        }


        if (!childTaskEntity.currentDepthCompleted.trim.toBoolean) {
          childTaskEntity.currentDepthCompleted = "true"
        }
        if (page || pageSize != -1) calPagesAndGenSubTaskEntitys(refStartPage, null, true, taskEntityList, docId, currentDepthCompleted, childTaskEntity, refu, page, pageSize, fistPageSize, total, refFirstUrl, totalPageSize, maxPage)
        else {
          childTaskEntity.taskURI = refu
          taskEntityList += childTaskEntity
        }
      }
    }
  }

  private def processPostRequest(refStartPage: Int, taskEntityList: ListBuffer[CrawlerTaskEntity], docId: String, currentRefDocId: String, currentDepthCompleted: Boolean, p: Map[String, AnyRef], refPostParamters: Map[String, AnyRef], childTaskEntity: CrawlerTaskEntity, refUrl: String, page: Boolean, pageSize: Int, fistPageSize: Int, total: Int,
                                 refDocFirstUrl: String, totalPageSize: Int, maxPage: Int): Unit = {
    var refURL = refUrl
    childTaskEntity.taskURI = refUrl

    childTaskEntity.httpmethod = HttpRequestMethodType.POST
    val paramsDataMap: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]

    //package post request
    for (postPara <- refPostParamters) {
      val postKey = postPara._1
      var postVal = postPara._2
      breakable {
        for (pos <- p) {
          if (Util.regexExtract(postVal.toString, Constant(conf).DOLLAR, 1).asInstanceOf[String].equalsIgnoreCase(pos._1)) {
            postVal = pos._2.asInstanceOf[String]
            break
          }
        }
      }
      paramsDataMap.put(postKey, postVal)
    }
    if (page || pageSize != -1) calPagesAndGenSubTaskEntitys(refStartPage, paramsDataMap, true, taskEntityList, docId, currentDepthCompleted, childTaskEntity, refUrl, page, pageSize, fistPageSize, total, refDocFirstUrl, totalPageSize, maxPage)
    else {
      postToTaskEntity(taskEntityList, childTaskEntity, paramsDataMap)
      taskEntityList += childTaskEntity
    }
  }

  def postToTaskEntity(taskEntityList: ListBuffer[CrawlerTaskEntity], childTaskEntity: CrawlerTaskEntity, paramsDataMap: util.Map[String, AnyRef]) = {
    val om: ObjectMapper = new ObjectMapper()
    val dataMap: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]
    dataMap.put(Constant(conf).PARAMSPATA, paramsDataMap)
    val params = om.writeValueAsString(dataMap)
    childTaskEntity.parentTaskToParames = params
  }

  def calPagesAndGenSubTaskEntitys(refStartPage: Int, paramsDataMap: util.Map[String, AnyRef], isGetMethod: Boolean, taskEntityList: ListBuffer[CrawlerTaskEntity], docId: String, currentDepthCompleted: Boolean, childTaskEntity: CrawlerTaskEntity, refUrl: String,
                                   page: Boolean, pageSize: Int, fistPageSize: Int, total: Int, refDocFirstUrl: String, totalPageSize: Int, maxPage: Int): Unit = {
    var reffL = refUrl
    if (page || pageSize != -1) {
      //need page

      if (pageSize != -1) {
        val tmpCurrentPage: Int = -1
        var tmpTotalPageCnt = totalPageSize
        var tmpPageSize = pageSize
        var tmpCurrentDepthCompleted = "false"
        var tmpFistPageSize = 0
        var startPage = refStartPage
        //caculage totalPageSize
        //have totalPageSize

        if (refDocFirstUrl != null) {

          if (isGetMethod) {
            val cloneChildTaskEntity = childTaskEntity.allCloneSelf()
            cloneChildTaskEntity.taskURI = refDocFirstUrl
            cloneChildTaskEntity.currentDepthCompleted = tmpCurrentDepthCompleted
            taskEntityList += cloneChildTaskEntity
          }
          //first web page
          //tmpTotalPageCnt = 1
          tmpFistPageSize = fistPageSize

          // startPage = 2
        }

        if (!currentDepthCompleted) startPage = 2

        tmpCurrentDepthCompleted = currentDepthCompleted.toString
        if (!childTaskEntity.currentDepthCompleted.trim.toBoolean) {
          tmpCurrentDepthCompleted = "true"
        }

        //other web pages
        if (total > -1 && tmpTotalPageCnt == -1) {
          //have total number
          tmpTotalPageCnt = if ((total - tmpFistPageSize) % tmpPageSize == 0) ((total - tmpFistPageSize) / tmpPageSize) else ((total - tmpFistPageSize) / tmpPageSize + 1)
        }
        //println(reffL+": total=>"+total+"tmpFistPageSize=>"+tmpFistPageSize+"tmpPageSize=>"+tmpPageSize+"tmpTotalPageCnt=>"+tmpTotalPageCnt)
        if (tmpTotalPageCnt != -1) {
          if (tmpTotalPageCnt > maxPage && maxPage != -1) tmpTotalPageCnt = maxPage
          for (i <- startPage to tmpTotalPageCnt) {
            val cloneChildTaskEntity = childTaskEntity.allCloneSelf()
            var tI = i
            if (refDocFirstUrl != null) tI = tI + 1
            if (isGetMethod) {
              cloneChildTaskEntity.taskURI = urlGenCurrentPage(tI, reffL)
            } else {
              //post
              breakable {
                val conVertMap = mapAsScalaMapConverter(paramsDataMap).asScala
                //for (postPara: util.Map.Entry[String, AnyRef] <- conVertMap) {
                for (postPara <- conVertMap) {
                  /*val postKey = postPara.getKey
                    var postVal = postPara.getValue*/
                  val postKey = postPara._1
                  var postVal = postPara._2
                  val tmpPostVal = urlGenCurrentPage(i, postVal.toString)
                  if (tmpPostVal != null) {
                    paramsDataMap.put(postKey, tmpPostVal)
                    break
                  }
                }
                postToTaskEntity(taskEntityList, cloneChildTaskEntity, paramsDataMap)
              }
            }
            cloneChildTaskEntity.currentDepthCompleted = tmpCurrentDepthCompleted
            taskEntityList += cloneChildTaskEntity
          }
        }
      }
    }
  }

  def urlGenCurrentPage(i: Int, refURL: String): String = {
    if (Util.regexExtract(refURL, Constant(conf).DOLLA_PAGE_GET).asInstanceOf[ListBuffer[String]].size == 1)
      refURL.replaceAll(Constant(conf).DOLLA_PAGE_GET, i.toString)
    else if ("page".equalsIgnoreCase(Util.regexExtract(refURL, "(\\$\\{page\\})", 1).asInstanceOf[String]))
      refURL.replaceAll("(\\$\\{page\\})", i.toString)
    else null
  }

  /* def testWorkerActor = {
       println("dddd")
       if(AgentWorker.workerActor!=null) AgentWorker.workerActor ! TestMsg("hello msg!")
     }*/
}

private[crawler] object SchemeTopicParser {

  var controlActor: ActorRef = null

  val cacheLoader: CacheLoader[java.lang.String, java.util.Map[String, SemanticNode]] =
    new CacheLoader[java.lang.String, java.util.Map[String, SemanticNode]]() {
      def load(key: java.lang.String): java.util.Map[String, SemanticNode] = {
        null
      }
    }

  val schemeDocCacheManager = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.HOURS).build(cacheLoader)

  def main(args: Array[String]) {
    //if(AgentWorker.workerActor!=null) AgentWorker.workerActor ! "hello msg!"
    // test2
    //testPageRegex
    //testScheme
    //testUrlRef
    //testUrlEncode

    //testFilterRegex
    //testFilterNot

    testLng
  }

  def testLng() = {

    val source = "src=http://apis.map.qq.com/ws/staticmap/v2/?key=I3OBZ-MBSRQ-WBJ5P-G5VZS-QGAIF-Y7B27&size=240*90&center={lat},{lng}&zoom=15&markers=icon:http({lng:116.518197,lat:39.923966})"
    val lng = "(\\{lng:.+,lat:.+\\d\\})"
    println(Util.regexExtract(source.toString(), lng, 1).asInstanceOf[String])

  }

  def testFilterNot() = {
    if (Util.find("sdfs中国", "中国")) println("true")
    else println("false")
  }

  def testFilterRegex() = {
    println(Util.regexExtract("/search/category/3/10/r62#nav-tab|0|1", "([\\w/]+)#", 1).asInstanceOf[String])
  }

  def testUrlEncode() = {
    var url: String = "http://www.dianping.com/search/category/4425/10#nav-tab|0|1"
    val model: String = "http://model"
    url = url.replaceFirst("http://", "")
    println(url)
    url = URLEncoder.encode(url, "utf-8")
    println(url)
    url = model.replaceFirst("model", url)
    println(url)

  }

  def testUrlRef = {
    println(Util.regexExtract("http://www.dianping.com/search/category/${cityid}/10#nav-tab|0|1", Constant(new CrawlerConf()).DOLLAR))
  }

  def testScheme() = {
    val t = new AnalyseSemanticTree()
    //t.newDocEl(ClassLoader.getSystemResourceAsStream("scheme_example/DemoDazhongdianping.xml"))
    t.newDocEl("E:\\scheme\\DemoDazhongdianping.xml")
    println(t.tree)
    println("finished!")
  }

  def test1() = {
    val name = Util.regexExtract("${name}", "\\$\\{(\\w+)\\}", 1).asInstanceOf[String]
    if (name != null) {
      if (name.equalsIgnoreCase("name")) {
        println("true")
      } else println("false")
    } else println("null")
  }

  def test2() = {
    var s1 = "ssss${yes}"
    var s2 = "(\\$\\{yes\\})"
    var s = "(\\$\\{name\\})"

    s = s.replaceAll("name", "yes")
    //var tt = "\\$\\{(name)\\}"

    //tt= tt.replaceAll("\\$\\{(name)\\}","d")
    //s = s.replaceAll("\\$\\{(name)\\}", "yes")
    println(s)

    s2 = s1.replaceAll(s2, "hh")

    println(s2)

    s1 = s1.replaceAll(s, "ii")
    println(s1)
  }

  def testPageRegex() = {
    var s1 = "http://www.dianping.com/lskjke${page}?aid=bf3133f6cf5079c59b8c8b315bcca026dc74ae45e529cc10368c3562c4b17f2715152b6bdfb29834f8e761cbec7f47c8cc713db7df194db5557cff3892124763190b89756f9e4156a7f2016980b11045&tc=2\""
    var s = "(\\$\\{[a-zA-z0-9]+\\})"

    val name = Util.regexExtract(s1, s).asInstanceOf[ListBuffer[String]]

    println(name.size)
    val s2 = s1
    println(s1.replaceAll(s, "d444"))

    def test1() = {
      val name = Util.regexExtract("${name}", "\\$\\{(\\w+)\\}", 1).asInstanceOf[String]
      if (name != null) {
        if (name.equalsIgnoreCase("name")) {
          println("true")
        } else println("false")
      } else println("null")
    }

    def test2() = {
      var s1 = "ssss${yes}"
      var s2 = "(\\$\\{yes\\})"
      var s = "(\\$\\{name\\})"

      s = s.replaceAll("name", "yes")
      //var tt = "\\$\\{(name)\\}"

      //tt= tt.replaceAll("\\$\\{(name)\\}","d")
      //s = s.replaceAll("\\$\\{(name)\\}", "yes")
      println(s)

      s2 = s1.replaceAll(s2, "hh")

      println(s2)

      s1 = s1.replaceAll(s, "ii")
      println(s1)
    }

    val pageR = "(\\$\\{page\\})"
    println(Util.regexExtract(s2, pageR, 1).asInstanceOf[String])
    println(s2.replaceAll(pageR, "3333333333333"))

  }
}
