package com.foofv.crawler.parse.topic

import com.foofv.crawler.parse.TopicParser
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.downloader.Response
import com.foofv.crawler.parse.xpath.HtmlSoldier
import com.foofv.crawler.util.Util
import com.foofv.crawler.parse.topic.entity.TakeoutMerchant
import org.jsoup.nodes.Element
import org.apache.commons.lang3.StringUtils
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.storage.StorageManager
import java.util.regex.Pattern
import com.foofv.crawler.parse.topic.entity.TakeoutMenu
import com.foofv.crawler.parse.topic.entity.TakeoutFood
import org.codehaus.jackson.map.ObjectMapper
import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer
import com.foofv.crawler.parse.topic.entity.Food
import com.foofv.crawler.parse.topic.entity.Menu
import com.foofv.crawler.enumeration.HttpRequestMethodType
import scala.collection.mutable.HashMap
import com.foofv.crawler.parse.topic.entity.Comment
import org.codehaus.jackson.JsonNode

class MeituanTakeoutMerchantTopicParser extends TopicParser {

  private val hs = HtmlSoldier()

  private def getHeadElement(hs: HtmlSoldier, soldier: String, element: Element): Element = {
    var result: Element = null
    if (element != null) {
      val div_selected = hs.%(soldier, element)
      if (div_selected != null && div_selected.length > 0) {
        result = div_selected.head
      }
    }
    result
  }

  private def getContentWrapped(hs: HtmlSoldier, soldier: String, element: Element): String = {
    var result = ""
    val contentEle = getHeadElement(hs, soldier, element)
    if (contentEle != null) {
      result = StringUtils.trim(contentEle.text())
    }
    result
  }

  private def getElementAttrWrapped(hs: HtmlSoldier, soldier: String, element: Element, attributeKey: String): String = {
    var result = ""
    val contentEle = getHeadElement(hs, soldier, element)
    if (contentEle != null) {
      result = StringUtils.trim(contentEle.attr(attributeKey))
    }
    result
  }

  private def getEleAttribute(element: Element, attributeKey: String): String = {
    var result = ""
    if (element != null && attributeKey != null) {
      result = StringUtils.trim(element.attr(attributeKey))
    }
    result
  }

  private def getScriptContent(element: Element): String = {
    var result = ""
    if (element != null) {
      result = StringUtils.trim(element.html())
    }
    result
  }

  override def parse1DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {
/*    var mttm: (Array[TakeoutMerchant], Boolean) = null
    if (obj != null) {
      mttm = obj.asInstanceOf[(Array[TakeoutMerchant], Boolean)]
    } else {
      mttm = geneNewTaskEntitySet1(resObj)._2.asInstanceOf[(Array[TakeoutMerchant], Boolean)]
    }
    mttm*/
    
    obj
  }

  override def parse2DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {
    /*var commentSeq: (Array[Comment], Boolean) = null
    if (obj != null) {
      commentSeq = obj.asInstanceOf[(Array[Comment], Boolean)]
      return commentSeq
    }
    try {
      // other comment pages
      val taskEntity = resObj.tastEntity
      val response: Response = resObj.response
      val content = Util.getResponseContent(response)
      val om: ObjectMapper = new ObjectMapper()

      val rootJsonNode = om.readTree(taskEntity.parentTaskToParames)
      val contextDataJsonNode = rootJsonNode.get("contextData")
      val merchantObjId = contextDataJsonNode.get("merchantObjId").getValueAsText
      val result = getMerchantComment(taskEntity, content, merchantObjId)
      if (result != null) {
        commentSeq = (result._1, true)
      }
    } catch {
      case t: Throwable => logError("parse2DepthPage error", t)
    }
    commentSeq*/
    
    obj
  }

  override def geneNewTaskEntitySet2(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    var comments: Array[Comment] = null
    var taskEntitys: Seq[CrawlerTaskEntity] = null
    try {
      val taskEntity = resObj.tastEntity
      val response: Response = resObj.response
      val content = Util.getResponseContent(response)
      val om: ObjectMapper = new ObjectMapper()

      val rootJsonNode = om.readTree(taskEntity.parentTaskToParames)
      val contextDataJsonNode = rootJsonNode.get("contextData")
      val merchantObjId = contextDataJsonNode.get("merchantObjId").getValueAsText

      val paramsDataJsonNode = rootJsonNode.get("paramsData")
      val wmpoiIdStr = paramsDataJsonNode.get("wmpoiIdStr").getValueAsText
      val offset = paramsDataJsonNode.get("offset").getValueAsText
      val has_content = paramsDataJsonNode.get("has_content").getValueAsText
      val score_grade = paramsDataJsonNode.get("score_grade").getValueAsText

      val result = getMerchantComment(taskEntity, content, merchantObjId)
      if (result != null) {
        comments = result._1
        
        if (offset.toInt < 2) {
          taskEntitys = for (index <- 2 to result._2) yield {
            val siblingTaskEntity = taskEntity.geneSiblingTaskEntity()

            val paramsDataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
            paramsDataMap.put("wmpoiIdStr", wmpoiIdStr)
            paramsDataMap.put("offset", "" + index)
            paramsDataMap.put("has_content", "0")
            paramsDataMap.put("score_grade", "0")

            val dataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
            dataMap.put("paramsData", paramsDataMap)

            val contextDataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
            contextDataMap.put("merchantObjId", merchantObjId)
            dataMap.put("contextData", contextDataMap)

            val params = om.writeValueAsString(dataMap)
            siblingTaskEntity.parentTaskToParames = params
            siblingTaskEntity.currentDepthCompleted = "true"
            siblingTaskEntity
          }
        }
      }
    } catch {
      case t: Throwable => logError("geneNewTaskEntitySet2 error", t)
    }
    (taskEntitys, (comments, true))
  }

  private def getMerchantComment(taskEntity: CrawlerTaskEntity, respContent: String, merchantObjId: String): (Array[Comment], Int) = {
    var commentBf: Buffer[Comment] = null
    try {
      var page_total = 0
      val om: ObjectMapper = new ObjectMapper()
      val respRootJsonNode = om.readTree(respContent)
      val code = respRootJsonNode.get("code").getValueAsInt
      if (code != 0) {
        val msg = getValueByFieldName("msg", respRootJsonNode).getOrElse("")
        val taskId = taskEntity.taskId
        logInfo(s"task[$taskId] http response json data: code[$code], msg[$msg]")
        null
      } else {
        val dataNode = respRootJsonNode.get("data")
        page_total = getValueByFieldName("page_total", dataNode).getOrElse("0").toInt
        val wmCommentVo = dataNode.get("wmCommentVo")
        if (wmCommentVo != null && wmCommentVo.isArray()) {
          commentBf = new ArrayBuffer[Comment]
          val commentIterator = wmCommentVo.iterator()
          while (commentIterator.hasNext()) {
            val commentJsonNode = commentIterator.next()
            val has_content_comment = getValueByFieldName("has_content_comment", commentJsonNode).getOrElse("0")
            val commentTime = getValueByFieldName("commentTime", commentJsonNode).getOrElse("")
            val wmCommentJsonNode = commentJsonNode.get("wmComment")
            val commentContent = getValueByFieldName("comment", wmCommentJsonNode).getOrElse("")
            val commentId = getValueByFieldName("id", wmCommentJsonNode).getOrElse("")
            val commentScore = getValueByFieldName("order_comment_score", wmCommentJsonNode).getOrElse("")
            val userId = getValueByFieldName("user_id", wmCommentJsonNode).getOrElse("")
            val username = getValueByFieldName("username", wmCommentJsonNode).getOrElse("")

            val comment = new Comment
            comment.setHasContent(has_content_comment)
            comment.setCommentTime(commentTime)
            comment.setCommentId(commentId)
            comment.setCommentContent(commentContent)
            comment.setCommentScore(commentScore)
            comment.setUserId(userId)
            comment.setUsername(username)
            comment.setMerchantId(merchantObjId)
            comment.setOriginSite(taskEntity.taskDomain)
            comment.setTaskId(taskEntity.taskId)
            comment.setBatchId(taskEntity.currentBatchId)
            comment.setJobId(taskEntity.jobId)
            comment.setJobName(taskEntity.jobName)

            //if (commentBf.length < 10) {
            commentBf += comment
            //}
          }
        }
      }
      if (commentBf != null) {
        (commentBf.toArray, page_total)
      } else {
        null
      }
    } catch {
      case t: Throwable => {
        logError("getMerchantComment error", t)
        null
      }
    }

  }

  override def geneNewTaskEntitySet1(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    val taskEntity = resObj.tastEntity
    val response: Response = resObj.response
    val content = Util.getResponseContent(response)
    val mttm = new TakeoutMerchant()
    mttm.setTaskId(taskEntity.taskId)
    mttm.setUrl(taskEntity.taskURI)
    mttm.setBatchId(taskEntity.currentBatchId)
    mttm.setJobId(taskEntity.jobId)
    mttm.setJobName(taskEntity.jobName)
    mttm.setOriginSite(taskEntity.taskDomain)
    val body = hs.%("body", content).head
    parseRestInfo(taskEntity, mttm, body)
    parseTakeoutMenu(taskEntity, mttm, body)
    parseWidgets(taskEntity, mttm, body)
    mttm.setMetaData(mttm.getName + "," + mttm.getAddress)

    // child-taskEntity comment
    val childTaskUrlTemplate = "http://waimai.meituan.com/comment/"
    val childTaskEntity = taskEntity.geneChildTaskEntity()
    childTaskEntity.httpRefer = childTaskUrlTemplate + mttm.getMerchantId
    childTaskEntity.taskURI = "http://waimai.meituan.com/ajax/comment"
    childTaskEntity.httpmethod = HttpRequestMethodType.POST
    childTaskEntity.currentDepthCompleted = "false"
    val om: ObjectMapper = new ObjectMapper()
    val paramsDataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
    paramsDataMap.put("wmpoiIdStr", mttm.getMerchantId)
    paramsDataMap.put("offset", "" + 1)
    paramsDataMap.put("has_content", "0")
    paramsDataMap.put("score_grade", "0")

    val dataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
    dataMap.put("paramsData", paramsDataMap)

    val contextDataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
    contextDataMap.put("merchantObjId", mttm.getObjectId)

    dataMap.put("contextData", contextDataMap)
    val params = om.writeValueAsString(dataMap)
    childTaskEntity.parentTaskToParames = params

    (Seq(childTaskEntity), (Array(mttm), false))
  }

  private def parseWidgets(taskEntity: CrawlerTaskEntity, mttm: TakeoutMerchant, body: Element) {
    try {
      val widget_broadcaster = getHeadElement(hs, "script#bulletin", body)
      if (widget_broadcaster != null) {
        val bulletin = widget_broadcaster.html().trim().replaceAll(Pattern.quote("<br />"), "\n").replaceAll(Pattern.quote("&nbsp;"), " ")
        mttm.setBulletin(bulletin)
      }

      val widget_discount = getHeadElement(hs, "div.wrapper div.page-wrap div.inner-wrap div.widgets.fr div.widget.discount", body)
      if (widget_discount != null) {
        var span_discount_desc = new ArrayBuffer[Element]()
        val span_discount_desc_exclude = new ArrayBuffer[String]()
        span_discount_desc.insertAll(0, hs.%("p span.discount-desc", widget_discount).toTraversable)

        val span_disCount_desc_delivery = getHeadElement(hs, "p:has(i.icon.i-delivery) span.discount-desc", widget_discount)
        if (span_disCount_desc_delivery != null) {
          span_discount_desc_exclude.append(span_disCount_desc_delivery.text())
        }

        val span_disCount_desc_delivery_fee = getHeadElement(hs, "p.delivery-fee span.discount-desc", widget_discount)
        if (span_disCount_desc_delivery_fee != null) {
          span_discount_desc_exclude.append(span_disCount_desc_delivery_fee.text())
        }

        val i_online_pay = getHeadElement(hs, "p i.icon.i-pay", widget_discount)
        if (i_online_pay != null) {
          mttm.setOnlinepaySupported("1")
          val span_online_pay = getHeadElement(hs, "p:has(i.icon.i-pay) span.discount-desc", widget_discount)
          if (span_online_pay != null) {
            span_discount_desc_exclude.append(span_online_pay.text())
          }
        }
        val i_cheque = getHeadElement(hs, "p i.icon.i-cheque", widget_discount)
        if (i_cheque != null) {
          mttm.setInvoiceSupported("1")
          val span_cheque = getHeadElement(hs, "p:has(i.icon.i-pay) span.discount-desc", widget_discount)
          if (span_cheque != null) {
            span_discount_desc_exclude.append(span_cheque.text())
          }
        }
        span_discount_desc = span_discount_desc.filter { x => span_discount_desc_exclude.indexOf(x.text()) == -1 }
        val discountCompain = hs.%("*", "text", span_discount_desc)
        mttm.setDiscountCompaign(discountCompain)
      }
    } catch {
      case t: Throwable => logError("parseWidgets error", t)
    }

  }

  private def parseRestInfo(taskEntity: CrawlerTaskEntity, mttm: TakeoutMerchant, body: Element) {
    try {
      val div_rest_info = getHeadElement(hs, "div.wrapper div.page-wrap div.inner-wrap div.rest-info", body)
      if (div_rest_info != null) {
        // rankScore
        mttm.setRankScore(getContentWrapped(hs, "div.ack-ti div.nu strong", div_rest_info))

        // averageSendTime
        mttm.setAverageSendTime(getContentWrapped(hs, "div.average-speed div.nu strong", div_rest_info))

        // intimeRate
        mttm.setIntimeRate(getContentWrapped(hs, "div.in-ti div.nu strong", div_rest_info))

        // saveUpCount
        mttm.setSaveUpCount(getContentWrapped(hs, "div.save-up-wrapper p.j-save-up-people span", div_rest_info))

        // logoUrl
        mttm.setLogoUrl(getElementAttrWrapped(hs, "div.details div.up-wrap div.avatar.fl img.scroll-loading", div_rest_info, "data-src"))

        // merchant id
        val linkUrl = getElementAttrWrapped(hs, "div.details div.up-wrap div.list div.na a", div_rest_info, "href")
        val id = linkUrl.split("/").last
        mttm.setMerchantId(id)

        //name
        mttm.setName(getContentWrapped(hs, "div.details div.up-wrap div.list div.na a span", div_rest_info))

        //rest-info-thirdpart
        val info_thirdpart = getContentWrapped(hs, "div.details div.up-wrap div.list div.clearfix div.rest-info-thirdpart span", div_rest_info)
        var temp = info_thirdpart.split(160.toChar + "", 3)
        if (temp.length != 3) {
          temp = info_thirdpart.split("\\s", 3)
        }
        if (temp.length == 3) {
          val deliveryStartPriceStr = temp(0)
          if (deliveryStartPriceStr.contains("元起送")) {
            mttm.setDeliveryStartPrice(deliveryStartPriceStr.replace("元起送", ""))
          }
          val deliveryPriceStr = temp(1)
          if (deliveryPriceStr.contains("免费配送")) {
            mttm.setSendPrice("0")
          } else if (deliveryPriceStr.contains("元配送费")) {
            mttm.setSendPrice(deliveryPriceStr.replace("元配送费", ""))
          }
          val deliveryProviderStr = temp(2)
          mttm.setDeliveryProvider(deliveryProviderStr)
        }

        // address
        mttm.setAddress(getContentWrapped(hs, "div.details div.rest-info-down-wrap div.location.fl span.info-detail", div_rest_info))

        // businessHours
        mttm.setBusinessHours(getContentWrapped(hs, "div.details div.rest-info-down-wrap div.delivery-time.fl span.info-detail", div_rest_info))
      }
    } catch {
      case t: Throwable => logError("parseRestInfo error", t)
    }

  }

  private def parseTakeoutMenu(taskEntity: CrawlerTaskEntity, mttm: TakeoutMerchant, body: Element) {
    try {
      val div_food_nav = getHeadElement(hs, "div.wrapper div.page-wrap div.inner-wrap div.food-list.fl div.food-nav", body)
      val div_food_categorys = hs.%("div.category", div_food_nav)
      if (div_food_categorys != null) {
        val cate_iterator = div_food_categorys.iterator
        val menuList = new java.util.ArrayList[Menu]()
        mttm.setMenus(menuList)
        while (cate_iterator.hasNext) {
          val div_category = cate_iterator.next()
          val menu = new TakeoutMenu
          menuList.add(menu)
          menu.setMerchantId(mttm.getObjectId)
          menu.setTaskId(taskEntity.taskId)
          menu.setBatchId(taskEntity.currentBatchId)
          menu.setJobId(taskEntity.jobId)
          menu.setJobName(taskEntity.jobName)
          menu.setOriginSite(taskEntity.taskDomain)
          menu.setMenuName(getElementAttrWrapped(hs, "h3.title", div_category, "title"))
          menu.setMenuDescription(getContentWrapped(hs, "div.food-cate-desc", div_category))
          val foodList = new java.util.ArrayList[Food]()
          menu.setFoods(foodList)
          val div_pic_food_conts = hs.%("div.pic-food-cont  div.j-pic-food.pic-food", div_category)
          for (div_food <- div_pic_food_conts) {
            try {
              parseTakeoutFood(taskEntity, menu, div_food)
            } catch {
              case t: Throwable => logError(s"parseTakeoutFood error div_food[$div_food]", t)
            }
          }

          val div_text_food_conts = hs.%("div.text-food-cont div.j-text-food.text-food", div_category)
          for (div_food <- div_text_food_conts) {
            try {
              parseTakeoutFood(taskEntity, menu, div_food)
            } catch {
              case t: Throwable => logError(s"parseTakeoutFood error div_food[$div_food]", t)
            }
          }
        }
      }
    } catch {
      case t: Throwable => logError("parseTakeoutMenu error", t)
    }

  }

  private def parseTakeoutFood(taskEntity: CrawlerTaskEntity, menu: TakeoutMenu, div_food: Element) {
    val foodList = menu.getFoods
    val food = new TakeoutFood
    foodList.add(food)
    food.setTaskId(taskEntity.taskId)
    food.setMenuId(menu.getObjectId)
    food.setBatchId(taskEntity.currentBatchId)
    food.setJobId(taskEntity.jobId)
    food.setJobName(taskEntity.jobName)
    food.setOriginSite(taskEntity.taskDomain)
    food.setSoldCount(getContentWrapped(hs, "div.ct-lightgrey span", div_food))
    val zan_count = getContentWrapped(hs, "div:has(i.i-zan) span.cc-lightred-new", div_food)
    food.setLikeCount(zan_count.replaceAll("[()]", ""))
    val img_food = getHeadElement(hs, "div.avatar img", div_food)
    if (img_food != null) {
      food.setImgUrl(StringUtils.trim(img_food.attr("data-src")))
      food.setFoodDescription(getContentWrapped(hs, "div.avatar div.description", div_food))
    }
    val script = getHeadElement(hs, "script", div_food)
    val content = getScriptContent(script)
    val mapper: ObjectMapper = new ObjectMapper();
    val rootNode = mapper.readTree(content)
    food.setFoodId(getValueByFieldName(("id"), rootNode).getOrElse(""))
    food.setFoodName(getValueByFieldName(("name"), rootNode).getOrElse(""))
    food.setPrice(getValueByFieldName(("price"), rootNode).getOrElse(""))
    food.setOnSale(getValueByFieldName(("onSale"), rootNode).getOrElse("0"))
    val skuJsonNode = rootNode.get("sku")
    if (skuJsonNode != null && skuJsonNode.isArray()) {
      val it = skuJsonNode.iterator()
      val temp: Buffer[String] = new ArrayBuffer[String]()
      while (it.hasNext()) {
        val se = it.next()
        temp += (se.toString())
      }
      food.setSku(temp.mkString(","))
    }
  }

  private def getValueByFieldName(field: String, jsonNode: JsonNode): Option[String] = {
    var result: String = null
    if (jsonNode != null) {
      val valueJsonNode = jsonNode.get(field)
      if (valueJsonNode != null) {
        result = valueJsonNode.getValueAsText
      }
    }
    Option(result)
  }

}

object TestMeituanTakeoutMerchantTopicParser {

  def main(args: Array[String]): Unit = {
    test
  }

  def test = {
    val conf: CrawlerConf = new CrawlerConf
    var storageManager: StorageManager = StorageManager("hbase", conf)
    //val resObj = storageManager.getByKey[ResObj]("0_1_0c42e2cc-207e-4d68-9aaa-30c4bbcdbe61")
    val resObj = storageManager.getByKey[ResObj]("a95a1b9c-588a-471f-8de5-4794d67064fe_1_0")
    val parser = new MeituanTakeoutMerchantTopicParser
    resObj.tastEntity.currentDepth = 1
    val result = parser.geneNewTaskEntitySet(resObj)
    System.err.println(result)
  }
}

