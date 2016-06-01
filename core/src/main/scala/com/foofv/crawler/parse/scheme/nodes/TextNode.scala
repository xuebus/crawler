package com.foofv.crawler.parse.scheme.nodes

import java.util.regex.{Matcher, Pattern}
import scala.collection.mutable.{ListBuffer, HashMap, Map}

import com.foofv.crawler.parse.scheme.{FastJsonImpl, JsonParser}
import com.foofv.crawler.util.Util
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import scala.collection.mutable
import scala.collection.mutable.StringBuilder

/**
 * Created by soledede on 2015/8/18.
 */
private[crawler]
class TextNode extends SemanticNode {


  var index: Int = 0
  var itemType: String = null
  var seprator: String = null


  def getSeprator(element: org.w3c.dom.Element) = {
    seprator = element.getAttribute("seprator").trim
    if (seprator.isEmpty) {
      seprator = null
    }
  }


  def getItemType(element: org.w3c.dom.Element) = {
    itemType = element.getAttribute("type").trim
    if (itemType.isEmpty) {
      itemType = null
    }
  }


  def getIndex(element: org.w3c.dom.Element) {
    val i: String = element.getAttribute("index").trim
    if (!i.isEmpty) {
      index = i.toInt
    }
  }


  override def exec(el: AnyRef, result: Map[String, AnyRef], p: Map[String, AnyRef], context: AnyRef, contextCache: Map[String, AnyRef]): AnyRef = {
    if (el == null) {
      return null
    }
    var total: Int = -1
    var obj: String = null

    var element: Element = null
    var elements: Elements = null
    var tmpElements: org.jsoup.select.Elements = null
    var elTextNode: org.jsoup.nodes.TextNode = null
    var elCp = el
    if (regex != null) elCp = Util.regexExtract(el.toString, regex, group).asInstanceOf[String]

    var contextElements: org.jsoup.select.Elements = null
    if (useContext && context.isInstanceOf[org.jsoup.nodes.Document]) {
      contextElements = context.asInstanceOf[org.jsoup.nodes.Document].getAllElements
    }
    else if (elCp.isInstanceOf[Element]) {
      element = elCp.asInstanceOf[Element]
    } else if (elCp.isInstanceOf[Elements]) {
      elements = elCp.asInstanceOf[Elements]
    } else if (elCp.isInstanceOf[org.jsoup.nodes.TextNode]) {
      elTextNode = elCp.asInstanceOf[org.jsoup.nodes.TextNode]
    }
    try {
      if (cssSelector != null && !cssSelector.isEmpty) {
        if (contextElements != null) {
          tmpElements = contextElements.select(cssSelector)
        }
        else if (element != null) {
          tmpElements = element.select(cssSelector)
        } else {
          tmpElements = element.select(cssSelector)
        }
        if (tmpElements == null || tmpElements.size() <= 0) return null
        if (itemType != null && itemType.trim.toLowerCase.startsWith("mult")) {
          val s = new StringBuilder
          for (ele <- 0 to tmpElements.size() - 1) {
            s ++= tmpElements.get(ele).text()
            s ++= seprator
          }
          obj = s.take(s.length - 1).toString()
        } else
          obj = tmpElements.get(index).text()
      } else if (jsonSelector != null && !jsonSelector.isEmpty) {
        var valueStr: String = null
        if (elCp.isInstanceOf[String]) {
          valueStr = FastJsonImpl(elCp.asInstanceOf[String]).select(jsonSelector).text()
        } else if (elCp.isInstanceOf[JsonParser]) {
          valueStr = elCp.asInstanceOf[JsonParser].select(jsonSelector).text()
        }
        if (seprator != null && !seprator.isEmpty && valueStr!=null) {
          val valueArray = valueStr.split(seprator)
          obj = valueArray(index).trim
        }
      } else {
        if (element != null) obj = element.text()
        else if (elements != null) {
          if (itemType != null && itemType.trim.toLowerCase.startsWith("mult")) {
            val s = new StringBuilder
            for (ele <- 0 to elements.size()) {
              s ++= elements.get(ele).text()
              s ++= seprator
            }
            obj = s.take(s.length - 1).toString()
          } else {
            if (last) obj = elements.get(elements.size() - 1).text()
            else {
              obj = elements.get(index).text()
            }
          }
        } else if (elTextNode != null) obj = elTextNode.text()
        else if (elCp.isInstanceOf[String]) {
          obj = elCp.asInstanceOf[String]
          if (seprator != null && !seprator.isEmpty) {
            val chaSeprator = seprator.toCharArray
            val valueArray = obj.split(chaSeprator)
            obj = valueArray(index).trim
          }
        }
      }
    } catch {
      case e: Throwable => logError("index maybe error", e)
    }
    var currentTotal: Int = -1
    var totalPageSize: Int = -1
    if (obj != null) {
      if (value != null) obj = value
      var regFilterObj: String = obj
      if (filterRegex != null)
        regFilterObj = Util.regexExtract(obj, filterRegex, filterGroup).asInstanceOf[String].trim

      if (name.toLowerCase.equalsIgnoreCase("total")) {
        total = regFilterObj.trim.toInt
      }
      else if (name.toLowerCase.equalsIgnoreCase("curtotal")) currentTotal = regFilterObj.toInt
      else if (name.toLowerCase.equalsIgnoreCase("totalpagesize")) totalPageSize = regFilterObj.toInt
      else {
        if (ifExist != null && default != null) {
          if (regFilterObj == null || regFilterObj.isEmpty) regFilterObj = default
          else regFilterObj = ifExist
        }
        else if (ifExist == null && default != null) regFilterObj = default
        
        if(replace !=null){
          if(replace.contains("->")){
         val rS = replace.split("->")
         if(regFilterObj != null)
         regFilterObj = regFilterObj.replaceAll(rS(0).trim, rS(1).trim)
          }
        }
        if (parameter) {
          p(name) = regFilterObj
        }
        if (!notSave) result(name) = regFilterObj
        if (needCache128) {
          if (regFilterObj.length <= 128) contextCache(name) = regFilterObj
          else logWarning("the length of your context cache content more than 128,we will not save it")
        }else if(needCache) contextCache(name) = regFilterObj

      }
    }
    (null, (total, currentTotal, totalPageSize))
  }


}

object TestRe{
  def main(args: Array[String]): Unit = {
    val i = """facade({entry:"app-shop-full-map-placeholder", data: {
        poi: "HESCRIZVVRIFJY",
        address: "\u897F\u575D\u6CB3\u4E2D\u8857\u4E346-7\u53F7",
        traffic: "\u5317\u4E09\u73AF\u9759\u5B89\u5E84\u7AD9\uFF1A\u72798\u5185\u5FEB\u3001683\u3001731\u3001848\u3001300\u5FEB\u5185\u3001300\u5185\u73AF\u3001302 \u5C55\u89C8\u4E2D\u5FC3\u7AD9\uFF1A104\u5FEB\u3001966\u3001515\u3001130\uFF1B\u5C55\u89C8\u4E2D\u5FC3\u603B\u7AD9\uFF1A18\u3001367\u3001596\uFF1B\u4E03\u5723\u5357\u8DEF\u7AD9\uFF08\u5F80\u5317\u5B98\u5385\u65B9\u5411\uFF09\uFF1A130",
        mapType: "7",
        park: false,
        mapReportData:{
            feedTitle:'[地图报错][北京]-[6674891]-[\u4E00\u5473\u4E00\u8BDA\u70E4\u5168\u9C7C(\u8001\u56FD\u5C55\u5E97)]',
            referShopID:6674891,
            referID:6674891,
            cityID:2
        },
        shopReportData:{
            feedTitle:'[商户报错][北京]-[6674891]-[\u4E00\u5473\u4E00\u8BDA\u70E4\u5168\u9C7C(\u8001\u56FD\u5C55\u5E97)]',
            referShopID:6674891,
            referID:6674891,
            cityID:2
        },
        shopId: 6674891,
        shopName: "一味一诚烤全鱼",
        userid: 0,
        cityId:2,
        cityCnName: "北京",
        usePOIbgFlag: "y",
        power:5,
        manaScore:0
    }});"""
    println(Util.regexExtract(i.toString, "traffic:.+",-1).asInstanceOf[String])
  }
}
