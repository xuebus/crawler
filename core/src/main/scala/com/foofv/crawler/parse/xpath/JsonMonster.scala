package com.foofv.crawler.parse.xpath

import java.util.regex.Pattern

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.jsoup.Jsoup

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.control.Breaks

/**
 * Created by msfenn on 14/07/15.
 */

class PairNode(val parentNode: JsonNode, val node: JsonNode) {
}

class JsonMonster(private var delimiter: String = ".", private var autoConversion: Boolean = true) {

  delimiter = Pattern.quote(delimiter)

  private val mapper = new ObjectMapper()

  def getValueFromJson(json: String, key: String): String = {

    getValueFromJson(json, key, false)
  }

  def getValueFromJson(json: String, key: String, isPedantic: Boolean): String = {

    getValueFromJsonCore(json, key, isPedantic, false)
    //    val keys = key.split(delimiter)
    //    val pairNode = getNodeFromJson(json, keys, isPedantic)
    //    if (pairNode != null)
    //      formatValue(pairNode.node.toString())
    //    else
    //      null
  }

  def getParentValueFromJson(json: String, key: String): String = {

    getParentValueFromJson(json, key, false)
  }

  def getParentValueFromJson(json: String, key: String, isPedantic: Boolean): String = {

    getValueFromJsonCore(json, key, isPedantic, true)
  }

  private def getValueFromJsonCore(json: String, key: String, isPedantic: Boolean, getParentValue: Boolean): String = {

    val keys = key.split(delimiter)
    val pairNode = getNodeFromJson(json, keys, isPedantic)
    if (pairNode != null) {
      if (getParentValue)
        formatValue(pairNode.parentNode.toString())
      else
        formatValue(pairNode.node.toString())
    }
    else
      null
  }

  def getValuesFromJson(json: String, key: String, level: Int): List[String] = {

    val values = ListBuffer[String]()
    val keys = key.split(delimiter)
    getValuesFromJson(json, keys, values, level)

    values.toList
  }

  def getValuesFromJson(json: String, key: String): List[String] = {

    getValuesFromJson(json, key, false)
  }

  def getValuesFromJson(json: String, key: String, isPedantic: Boolean): List[String] = {

    var values = ListBuffer[String]()
    val keys = key.split(delimiter)
    if (isPedantic) {
      val pairNode = getNodeFromJson(json, keys, isPedantic)
      if (pairNode != null) {
        if (pairNode.parentNode.isArray()) {
          var tmpNode: JsonNode = null
          for (i <- 0 to pairNode.parentNode.size() - 1) {
            tmpNode = pairNode.parentNode.get(i).get(keys(keys.length - 1))
            if (tmpNode != null)
              values += (formatValue(tmpNode.toString()))
          }
        } else
          values += (formatValue(pairNode.node.toString()))
      }
    } else
      getValuesFromJson(json, keys, values, 1)

    values.toList
  }

  private def getNodeFromJson(json: String, keys: Array[String], isPedantic: Boolean): NodePair /*throws IOException*/ = {

    val rootNode = mapper.readTree(json)
    var parentNode: JsonNode = null
    val pathDepth = keys.length
    var i = 0
    var node = if (isPedantic) rootNode.get(keys(i)) else rootNode.findValue(keys(i))
    i += 1
    while (node != null && node.isContainerNode() && i < pathDepth) {
      parentNode = node
      if (node.isArray() && isPedantic) {
        var tmpNode = node
        Breaks.breakable {
          for (j <- 0 to node.size()) {
            tmpNode = node.get(j).get(keys(i))
            if (tmpNode != null) {
              Breaks.break
            }
          }
        }
        node = tmpNode
        i += 1
      } else {
        node = if (isPedantic) node.get(keys(i)) else node.findValue(keys(i))
        i += 1
      }
    }
    if (node != null && i == pathDepth)
      new NodePair(parentNode, node)
    else
      null
  }

  private def getValuesFromJson(json: String, keys: Array[String], values: ListBuffer[String], level: Int): Unit = {

    val rootNode = mapper.readTree(json)
    getValuesFromJson(rootNode, keys, 0, values, level)
  }

  private def getValuesFromJson(parentNode: JsonNode, keys: Array[String], depth: Int, values: ListBuffer[String], level: Int): Unit = {

    val pathDepth = keys.length
    val nodes = parentNode.findValues(keys(depth))
    for (node <- nodes) {
      if (node != null && node.isContainerNode() && depth < pathDepth - 1)
        getValuesFromJson(node, keys, depth + 1, values, level)
      else if (node != null && depth == pathDepth - 1) {
        if (autoConversion && node.isArray()) {
          getValuesFromArrayNode(node, values, 1, level)
        }
        else
          values += formatValue(node.toString())
      }
    }
  }

  //  private def getValuesFromArrayNode(node: JsonNode, values: ListBuffer[String]) = {
  //
  //    for (i <- 0 to node.size() - 1) {
  //      values += (formatValue(node.get(i).toString()))
  //    }
  //  }

  private def getValuesFromArrayNode(node: JsonNode, values: ListBuffer[String], currentLevel: Int, level: Int): Unit = {

    var size: Int = 0
    for (i <- 0 to node.size() - 1) {
      size = values.size
      val subNode = node.get(i)
      if (subNode.isArray && currentLevel < level)
        getValuesFromArrayNode(subNode, values, currentLevel + 1, level)
      else
        values += (formatValue(node.get(i).toString()))
      if (currentLevel == 1 && level > 1) {
        //TODO: add this temporarily, afterwards it may be changed
        values.insert(0, "" + (values.size - size))
        //        values += "SPRT_FUCK@|@FUCK_SPRT"
      }
    }
    if (currentLevel == 1 && level > 1)
      values.insert(0, "" + node.size)
  }

  private def formatValue(value: String): String = {

    if (value.startsWith("\""))
      value.substring(1, value.length() - 1)
    else
      value
  }
}

object JsonMonster {

  def convert2Chinese(data: String): String = {

    val regex = "\\\\u(\\w{4})"
    var result = ""
    val matcher = Pattern.compile(regex).matcher(data)
    val bytes = ArrayBuffer[Byte](0, 0)
    var lastPos = 0
    var currPos = 0
    while (matcher.find()) {
      val hex = Integer.parseInt(matcher.group(1), 16)
      bytes(0) = ((hex & 0xFF00) >> 8).asInstanceOf[Byte]
      bytes(1) = (hex & 0xFF).asInstanceOf[Byte]
      currPos = matcher.start()
      result += data.substring(lastPos, currPos)
      result += new String(bytes.toArray, "UTF-16")
      lastPos = matcher.end()
    }
    if (result.isEmpty())
      data
    else
      result
  }
}

object JsonMonsterTest extends App {

  //  val doc = Jsoup.connect("http://t.cn/zjYwrl3").timeout(20000).get()
  //  println(doc.toString)

  var shopsURL = "http://waimai.baidu.com/mobile/waimai?qt=shoplist&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&page=1&display=json"
  var shopURL = "http://waimai.baidu.com/mobile/waimai?qt=shopmenu&shop_id=0&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&display=json"
  var shops = new ListBuffer[String]()
  var doc = Jsoup.connect(shopsURL).ignoreContentType(true).timeout(20000).execute().body()
  val parser = new JsonMonster()
  for (x <- parser.getValuesFromJson(doc, "result.shop_info.welfare_basic_info")) {
    if (x.contains("pay"))
      println(parser.getValueFromJson(x, "msg"))
  }
  println(parser.getValueFromJson(doc, "params.address"))
  //  shops ++= parser.getValuesFromJson(doc, "result.shop_info.brand" /*"shop_id" "shop_name"*/)
  //  val total = Integer.parseInt(parser.getValueFromJson(doc, "result.total"))
  //  println("total=" + total)
  //  val requests = total / 20 + {
  //    if (total % 20 == 0) 0 else 1
  //  }
  //  println("requests=" + requests)
  //  for (i <- 2 to requests) {
  //    val regex = "page=\\d+"
  //    shopsURL = shopsURL.replaceAll(regex, "page=" + i)
  //    doc = Jsoup.connect(shopsURL).ignoreContentType(true).timeout(20000).execute().body()
  //    shops ++= parser.getValuesFromJson(doc, "shop_name")
  //    Thread.sleep(1000)
  //  }
  //  shopURL = shopURL.replaceAll("shop_id=(\\w+)", "shop_id=" + shops(0))
  //  println(shopURL)
  //  var shopDoc = Jsoup.connect(shopURL).ignoreContentType(true).timeout(20000).execute().body()
  //  shops.clear()
  //  shops ++= parser.getValuesFromJson(shopDoc, "name")
  //  var i = 1
  //  for (shop <- shops) {
  //    println(i + ": " + shop)
  //    i += 1
  //  }
}