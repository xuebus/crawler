package com.foofv.crawler.parse.scheme.nodes

import java.util.Collections
import com.alibaba.fastjson.JSON
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.w3c.dom.Element
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, HashMap, Map}
import scala.util.control.Breaks._
import com.foofv.crawler.parse.scheme.ResultParameter

/**
 * Created by soledede on 2015/8/19.
 */
private[crawler]
class DocumentNode extends SemanticNode {

  var docId: String = null

  var firstDoc: Boolean = false

  var preDocId: String = null

  var refDocId: String = null

  var docUrl: String = null

  var docFirstUrl: String = null

  var method: String = "get"

  var page: Boolean = false

  var pageSize: Int = -1

  var firstPagesize: Int = -1

  var intelligent: Boolean = false

  var postParameters: Map[String, AnyRef] = null

  var tableName: String = "sys_soledede"

  var total: Int = -1

  var currentTotal = -1

  var totalPageSize: Int = -1

  var paramterHasList: Boolean = false

  var currentSubDoc: Int = -1

  var maxPage: Int = -1

  var sql: String = null

  var isStream: Boolean = false
  
  var startPage: Int = 1


  def getTableName(table: String, element: org.w3c.dom.Element) = {
    var tmp = element.getAttribute("table").trim
    if (tmp.isEmpty) tmp = element.getAttribute("tablename").trim
    if (tmp.isEmpty) tmp = element.getAttribute("tableName").trim
    if (tmp.isEmpty) {
      if (table != null && !table.isEmpty) tmp = table.trim
    }
    if (!tmp.isEmpty) tableName = tmp
  }

  def getDocId(element: org.w3c.dom.Element) = {
    docId = element.getAttribute("docid").trim
    if (docId.isEmpty) docId = element.getAttribute("docId").trim
    if (docId.isEmpty) docId = null
  }

  def getDocUrl(element: org.w3c.dom.Element) = {
    docUrl = element.getAttribute("url").trim
    if (docUrl.isEmpty) docUrl = null
  }

  def getSql(element: org.w3c.dom.Element) = {
    sql = element.getAttribute("sql").trim
    if (sql.isEmpty) sql = null
  }


  def getDocFirstUrl(element: org.w3c.dom.Element) = {
    docFirstUrl = element.getAttribute("firsturl").trim
    if (docFirstUrl.isEmpty) docFirstUrl = element.getAttribute("firstUrl").trim
    if (docFirstUrl.isEmpty) docFirstUrl = null
  }

  def getMethod(element: org.w3c.dom.Element) = {
    method = element.getAttribute("method").trim
    if (method.isEmpty) method = "get"
  }


  def getPage(element: org.w3c.dom.Element) = {
    val tmp = element.getAttribute("page").trim
    if (tmp.isEmpty) page = false
    else page = tmp.toBoolean
  }

  def getIsStream(element: org.w3c.dom.Element) = {
    var tmp = element.getAttribute("isStream").trim
    if (tmp.isEmpty) tmp = element.getAttribute("stream").trim
    if (tmp.isEmpty) tmp = element.getAttribute("isstream").trim
    if (tmp.isEmpty) isStream = false
    else isStream = tmp.toBoolean
  }


  def getPageSize(element: org.w3c.dom.Element): Int = {
    var tmp = element.getAttribute("pagesize")
    if (tmp.isEmpty) tmp = element.getAttribute("pageSize")
    if (!tmp.isEmpty) pageSize = tmp.trim.toInt
    pageSize
  }
  
   def getStartPage(element: org.w3c.dom.Element): Int = {
    var tmp = element.getAttribute("startPage")
    if (tmp.isEmpty) tmp = element.getAttribute("startpage")
    if (!tmp.isEmpty) startPage = tmp.trim.toInt
    startPage
  }

  def getFirstPagesize(element: org.w3c.dom.Element) = {
    var tmp = element.getAttribute("firstpagesize")
    if (tmp.isEmpty) tmp = element.getAttribute("firstPagesize")
    if (!tmp.isEmpty) firstPagesize = tmp.trim.toInt
    if (firstPagesize == -1) {
      firstPagesize = getPageSize(element)
    }
  }


  def getIntelligent(element: org.w3c.dom.Element) = {
    val tmp = element.getAttribute("intelligent").trim
    if (tmp.isEmpty) intelligent = false
    else intelligent = tmp.toBoolean
  }

  def getFirstDoc(element: org.w3c.dom.Element) = {
    var tmp = element.getAttribute("firstdoc")
    if (tmp.isEmpty) tmp = element.getAttribute("firstDoc")
    if (!tmp.isEmpty) firstDoc = tmp.trim.toBoolean
  }

  def getmaxPage(element: org.w3c.dom.Element) = {
    var tmp = element.getAttribute("maxpage")
    if (tmp.isEmpty) tmp = element.getAttribute("maxPage")
    if (!tmp.isEmpty) maxPage = tmp.trim.toInt
  }

  def process(el: AnyRef): AnyRef = {
    exec(el, null, null, null, null)
  }

  override def exec(el: AnyRef, result: Map[String, AnyRef], pa: Map[String, AnyRef], context: AnyRef, contextCache: Map[String, AnyRef]): AnyRef = {
    //init
    refDocId = null
    var paramterHasList = false
    var total = -1
    var currentTotal = -1
    var totalPageSize = -1


    var obj = new HashMap[String, AnyRef] //return object to save
    var parameter = new HashMap[String, AnyRef] //return paramter to request next web page
    var contextCache = new HashMap[String, AnyRef]

    var docResult: AnyRef = null

    var objs: AnyRef = null

    if (el.isInstanceOf[String]) {
      try {
        val j = JSON.parse(el.asInstanceOf[String])
        if (j.isInstanceOf[JSON]) {
          objs = j.asInstanceOf[JSON]
        } else {
          objs = Jsoup.parse(el.asInstanceOf[String])
        }
      }
      catch {
        case _ =>
          objs = Jsoup.parse(el.asInstanceOf[String])
      }
    }

    /* else if (el.isInstanceOf[org.jsoup.nodes.Document])
       objs = Collections.singletonList[org.jsoup.nodes.Element](el.asInstanceOf[org.jsoup.nodes.Document])*/
    for (i <- 0 to this.children.size - 1) {
      val child = this.children(i)
      if (child.isInstanceOf[ListNode]) {
        val listChildren = child.asInstanceOf[ListNode]
        if (listChildren.isFiled) {
          paramterHasList = true
          //need save as one property
          /**
           * {
           * name:xxx,
           * list:[
           * {}
           * {}
           * ]
           * }
           */
          val chList = listChildren.exec(objs, null, null, objs, null).asInstanceOf[(Seq[Map[String, String]], Seq[Map[String, String]], String)]
          if (chList != null) {
            val v = chList._1
            val rp = chList._2
            val name = chList._3
            if (v != null && v.size > 0)
              obj(name) = v
            if (rp != null && rp.size > 0)
              parameter(name) = rp
          }
        } else {
          docResult = listChildren.exec(objs, null, null, objs, null)
          paramterHasList = false
        }
      } else if (child.isInstanceOf[ReferenceNode]) {
        refDocId = child.asInstanceOf[ReferenceNode].exec(objs, null, null, objs, null).asInstanceOf[String]
      } else if (child.isInstanceOf[PostNode]) {
        //val parameters: Map[String, AnyRef] = child.asInstanceOf[PostNode].exec(el, null, null).asInstanceOf[Map[String, AnyRef]]
        // if (parameters != null) postParameters = parameters.map(x => (x._1.toString, x._2.toString))
      } else if (child.isInstanceOf[TextNode]) {
        val objectResult = child.exec(objs, obj, parameter, objs, contextCache).asInstanceOf[(AnyRef, (Int, Int, Int))]
        if (objectResult != null && objectResult._2 != null) {
          val tmpForPage = objectResult._2.asInstanceOf[(Int, Int, Int)]
          
          total = tmpForPage._1
          currentTotal = tmpForPage._2
          totalPageSize = tmpForPage._3
        }
      } else if (child.isInstanceOf[StringNode] || child.isInstanceOf[AttributeNode]) {
        child.exec(objs, obj, parameter, objs, contextCache)
      } else {
        docResult = child.exec(objs, obj, parameter, objs, contextCache).asInstanceOf[(AnyRef, Int)]
      }
    }
    val paraResult = ResultParameter(paramterHasList,total,currentTotal,totalPageSize)
    if (docResult != null && !docResult.isInstanceOf[Map[String, AnyRef]]){
      val lR = docResult.asInstanceOf[(_,_,_)]
      return (lR._1,lR._2,lR._3,paraResult)
    }
    else {
      if (obj.isEmpty) obj = null
      if (parameter.isEmpty) parameter = null
      if (contextCache.isEmpty) contextCache = null
      if (obj == null && parameter == null && contextCache == null) return null
      (obj, parameter, contextCache,paraResult)
    }
  }

  def extractPost() = {
    breakable {
      for (i <- 0 to this.children.size - 1) {
        val child = this.children(i)
        if (child.isInstanceOf[PostNode]) {
          postParameters = child.asInstanceOf[PostNode].exec(null, null, null, null, null).asInstanceOf[Map[String, AnyRef]]
          //if (parameters != null) postParameters = parameters.map(x => (x._1.toString, x._2.toString))
          break
        }
      }
    }
  }
}
