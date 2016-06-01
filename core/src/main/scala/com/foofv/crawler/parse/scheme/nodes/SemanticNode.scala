package com.foofv.crawler.parse.scheme.nodes

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.constant.Constant
import com.foofv.crawler.util.{Util, Logging}
import org.w3c.dom.Element

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{ListBuffer, HashMap, Map}

/**
 * Created by soledede on 2015/8/18.
 */
private[crawler]
abstract class SemanticNode extends Logging with Serializable {
  val conf = new CrawlerConf()

  var children = new ListBuffer[SemanticNode]

  var name: String = null

  var cssSelector: String = null

  var jsonSelector: String = null

  var parameter: Boolean = false

  var notSave: Boolean = false

  var regex: String = null
  var group: Int = -1

  var filterRegex: String = null
  var filterGroup = -1

  var filterNot: String = null

  var useContext: Boolean = false

  var needCache128: Boolean = false

  var needCache: Boolean = false

  var ifExist: String = null

  var default: String = null

  var last: Boolean = false

  var value: String = null
  
  var replace: String = null

  def getName(element: org.w3c.dom.Element) = {
    name = element.getAttribute("name").trim
    if (name.isEmpty) name = null
  }
  
  def getReplace(element: org.w3c.dom.Element) = {
    replace = element.getAttribute("replace").trim
    if (replace.isEmpty) replace = null
  }
  

  def getValue(element: org.w3c.dom.Element) = {
    value = element.getAttribute("value")
    if (value != null)
      value = value.trim
    if (value.isEmpty) value = null
    if (value == null) {
      val tag = element.getFirstChild
      if (tag != null) {
        var tagValue = tag.getNodeValue
        if (tagValue != null) tagValue = tagValue.trim
        if (tagValue != null && !tagValue.isEmpty) value = tagValue
      }
    }
  }

  def getCSSSelector(element: org.w3c.dom.Element) {
    cssSelector = element.getAttribute("selector")
    if (cssSelector.isEmpty) {
      cssSelector = null
    }
  }


  def getUseContext(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("usecontext").trim
    if (i.isEmpty) i = element.getAttribute("useContext").trim
    if (!i.isEmpty) {
      useContext = i.toBoolean
    }
  }


  def getJsonSelector(element: org.w3c.dom.Element) {
    jsonSelector = element.getAttribute("jsonselector").trim
    if (jsonSelector.isEmpty) jsonSelector = element.getAttribute("jsonSelector").trim
    if (jsonSelector == null || jsonSelector.isEmpty) {
      jsonSelector = name
    }
    if (jsonSelector != null)
      if (Util.regexExtract(jsonSelector.trim, Constant(conf).DOLLAR, 1).asInstanceOf[String].equalsIgnoreCase("name")) jsonSelector = name
  }

  def getParameter(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("parameter").trim
    if (i.isEmpty) {
      i = element.getAttribute("par").trim
    }
    if (!i.isEmpty) {
      parameter = i.trim.toBoolean
    }
  }

  def getnotSave(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("notsave").trim
    if (i.isEmpty) {
      i = element.getAttribute("notSave").trim
    }
    if (!i.isEmpty) {
      notSave = i.toBoolean
    }
  }

  def getLast(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("getLast").trim
    if (i.isEmpty) {
      i = element.getAttribute("getlast").trim
    }
    if (i.isEmpty) {
      i = element.getAttribute("last").trim
    }
    if (!i.isEmpty) {
      last = i.toBoolean
    }
  }


  def getFilterRegex(element: org.w3c.dom.Element) = {
    filterRegex = element.getAttribute("filterregex").trim
    if (filterRegex.isEmpty) {
      filterRegex = element.getAttribute("filterRegex").trim
      if (filterRegex.isEmpty) {
        filterRegex = null
      }
    }
  }

  def getfilterNot(element: org.w3c.dom.Element) = {
    filterNot = element.getAttribute("filternot").trim
    if (filterNot.isEmpty) {
      filterNot = element.getAttribute("filterNot").trim
      if (filterNot.isEmpty) {
        filterNot = null
      }
    }
  }

  def getGroup(element: org.w3c.dom.Element) = {
    val tmp = element.getAttribute("group").trim
    if (tmp != null) {
      if (tmp.isEmpty) {
        group = -1
      } else group = tmp.toInt
    } else group = -1

  }


  def getFilterGroup(element: org.w3c.dom.Element) = {
    var tmp = element.getAttribute("filtergroup")
    if (tmp.isEmpty) tmp = element.getAttribute("filterGroup").trim
    if (!tmp.isEmpty) {
      filterGroup = tmp.toInt
    }

  }

  def getRegex(element: org.w3c.dom.Element) = {
    regex = element.getAttribute("regex").trim
    if (regex.isEmpty) {
      regex = null
    }
  }

  def getNeedCache128(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("needcache128").trim
    if (i.isEmpty) {
      i = element.getAttribute("needCache128").trim
    }
    if (!i.isEmpty) {
      needCache128 = i.toBoolean
    }
  }

  def getNeedCache(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("needcache").trim
    if (i.isEmpty) {
      i = element.getAttribute("needCache").trim
    }
    if (!i.isEmpty) {
      needCache = i.toBoolean
    }
  }
  def getIfExist(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("ifexist").trim
    if (i.isEmpty) {
      i = element.getAttribute("ifExist").trim
    }
    if (i.isEmpty) {
      i = element.getAttribute("ifExists").trim
    }
    if (i.isEmpty) {
      i = element.getAttribute("ifexists").trim
    }

    if (!i.isEmpty) {
      ifExist = i.trim
    }
  }

  def getDefault(element: org.w3c.dom.Element) {
    var i: String = element.getAttribute("default").trim
    if (i.isEmpty) {
      i = element.getAttribute("DEFAULT").trim
    }

    if (!i.isEmpty) {
      default = i.trim
    }
  }


  //el: JsoupDoument or Json ...
  //(AnyRef, AnyRef)._1 result to save (Map(Stirng->String))
  //(AnyRef, AnyRef)._2 parameter to request next web page (Map(Stirng->String))
  //(AnyRef, AnyRef)._3 if page needed
  def exec(el: AnyRef, obj: Map[String, AnyRef], p: Map[String, AnyRef], context: AnyRef, contextCache: Map[String, AnyRef]): AnyRef

  def +(node: SemanticNode) = children.append(node)

  def hasChild(): Boolean = children.size > 0
}
