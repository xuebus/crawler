package com.foofv.crawler.parse.topic

import com.foofv.crawler.parse.TopicParser
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.downloader.Response
import com.foofv.crawler.util.Util
import com.foofv.crawler.parse.xpath.HtmlSoldier
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.codehaus.jackson.map.ObjectMapper
import com.foofv.crawler.parse.topic.entity.FoodMerchant
import com.foofv.crawler.parse.topic.entity.SpecialityMenu
import com.foofv.crawler.parse.topic.entity.SpecialityFood
import com.foofv.crawler.parse.topic.entity.Food
import com.foofv.crawler.parse.topic.entity.Menu

private[crawler] class MeituanFoodMerchantParser extends TopicParser {

  private def getEleAttribute(element: Element, attributeKey: String): String = {
    var result = ""
    if (element != null && attributeKey != null) {
      result = StringUtils.trim(element.attr(attributeKey))
    }
    result
  }

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

  private def getElementAttrWrapped(hs: HtmlSoldier, soldier: String, element: Element, attributeKey: String): String = {
    var result = ""
    val contentEle = getHeadElement(hs, soldier, element)
    if (contentEle != null) {
      result = StringUtils.trim(contentEle.attr(attributeKey))
    }
    result
  }

  val hs = HtmlSoldier()

  override def parse1DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {
    null
  }

  override def parse2DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {
    null
  }

  override def parse3DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {
    null
  }

  override def parse4DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {
    var merchantArray: (Array[FoodMerchant], Boolean) = null
    if (obj != null) {
      merchantArray = obj.asInstanceOf[(Array[FoodMerchant], Boolean)]
    }
    merchantArray
  }

  // city list page
  override def geneNewTaskEntitySet1(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    val taskEntity = resObj.tastEntity
    val response: Response = resObj.response
    val content = Util.getResponseContent(response)
    val allOnlineCityLinkeEles = hs.%("body div#bd div.citieslist a.isonline", content)
    val childrenTaskBuffer = new ArrayBuffer[CrawlerTaskEntity]
    val childTaskUrlTemplate = "@/shops/meishi?mtt=1.index%2Ffloornew.nc.1.idgrnx9y"
    allOnlineCityLinkeEles.foreach { cityLink =>
      {
        val href = getEleAttribute(cityLink, "href")
        if (StringUtils.isNotBlank(href) && href.startsWith("http://bj.meituan.com")) {
          val task = taskEntity.geneChildTaskEntity()
          task.taskURI = childTaskUrlTemplate.replace("@", href)
          childrenTaskBuffer += task
        }
      }
    }
    (childrenTaskBuffer.toSeq, null)
  }

  // city districts page
  override def geneNewTaskEntitySet2(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    val taskEntity = resObj.tastEntity
    val response: Response = resObj.response
    val content = Util.getResponseContent(response)
    val allDistrictLinkeEles = hs.%("body div#filter div.geo-filter-wrapper ul.inline-block-list a", content)
    val childrenTaskBuffer = new ArrayBuffer[CrawlerTaskEntity]
    val childTaskUrlTemplate = "@?mtt=1.shops%2Fdefault.zb.1.idgwbgh5"
    allDistrictLinkeEles.foreach { districtLink =>
      {
        val href = getEleAttribute(districtLink, "href")
        if (StringUtils.isNotBlank(href) && href.startsWith("http://bj.meituan.com/shops/meishi/tongzhou") ) {
          val task = taskEntity.geneChildTaskEntity()
          task.taskURI = childTaskUrlTemplate.replace("@", href)
          childrenTaskBuffer += task
        }
      }
    }
    (childrenTaskBuffer.toSeq, null)
  }

  // city district trade-areas page
  override def geneNewTaskEntitySet3(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    val taskEntity = resObj.tastEntity
    val response: Response = resObj.response
    val content = Util.getResponseContent(response)
    val childrenTaskBuffer = new ArrayBuffer[CrawlerTaskEntity]
    val allDistrictLinkeEles = hs.%("body div#filter div.geo-filter-wrapper ul.inline-block-list a", content)
    if (allDistrictLinkeEles.isEmpty) {
      val taskSeq = getFoodMerchantDetailPageTask(taskEntity, content)
      (taskSeq, null)
    } else {
      val childTaskUrlTemplate = "@?mtt=1.shops%2Fdefault.zb.1.idgwg35m"
      allDistrictLinkeEles.foreach { districtLink =>
        {
          val href = getEleAttribute(districtLink, "href")
          if (StringUtils.isNotBlank(href) && href.startsWith("http://bj.meituan.com/shops/meishi/tongzhoubeiyua") ) {
            val task = taskEntity.geneSiblingTaskEntity()
            task.taskURI = childTaskUrlTemplate.replace("@", href)
            task.currentDepthCompleted = "false"
            childrenTaskBuffer += task
            println("district---" + task.taskURI)
          }
        }
      }
      (childrenTaskBuffer.toSeq, null)
    }
  }

  private def getFoodMerchantDetailPageTask(taskEntity: CrawlerTaskEntity, content: String): Seq[CrawlerTaskEntity] = {
    val childrenTaskBuffer = new ArrayBuffer[CrawlerTaskEntity]
    childrenTaskBuffer ++= getMerchantDetailTaskEntity(taskEntity, content)
    childrenTaskBuffer ++= getPagingListTaskEntity(taskEntity, content)
    childrenTaskBuffer.toSeq
  }

  var merchantCounterA = 0
  private def getMerchantDetailTaskEntity(taskEntity: CrawlerTaskEntity, content: String): ArrayBuffer[CrawlerTaskEntity] = {
    val childrenTaskBuffer = new ArrayBuffer[CrawlerTaskEntity]
    try {
      val om: ObjectMapper = new ObjectMapper()
      val merchant_divs = hs.%("div#content div.J-shop-item.shop-item", content)
      if (merchant_divs != null) {
        val childTaskUrlTemplate = "@?mtt=1.shops%2Fdefault.0.0.idh2s8x5"
        merchant_divs.foreach { merchant_div =>
          {
            // url
            val href = getElementAttrWrapped(hs, "div.shop-meta h3 a.shop-meta__name", merchant_div, "href")
            if (StringUtils.isNotBlank(href)) {
              val task = taskEntity.geneChildTaskEntity()
              task.taskURI = childTaskUrlTemplate.replace("@", href)
              childrenTaskBuffer += task
              merchantCounterA += 1
              logInfo(s"getMerchantDetailTaskEntity [$merchantCounterA] " + task.taskURI)
              // merchant tags
              val tagAs = hs.%("div.shop-meta span.shop-meta__tags a", merchant_div)
              if (tagAs != null) {
                val tags = for (tagA <- tagAs) yield {
                  tagA.text()
                }
                if (tags != null) {
                  val dataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
                  val contextDataMap: java.util.Map[String, AnyRef] = new java.util.HashMap[String, AnyRef]
                  contextDataMap.put("tags", tags.mkString(" "))
                  dataMap.put("contextData", contextDataMap)
                  val params = om.writeValueAsString(dataMap)
                  task.parentTaskToParames = params
                }
              }
            }
          }
        }
      }
    } catch {
      case t: Throwable => logError("getMerchantDetailTaskEntity error", t)
    }
    //childrenTaskBuffer.clear()
    childrenTaskBuffer
  }

  var PagingListCounter = 0
  private def getPagingListTaskEntity(taskEntity: CrawlerTaskEntity, content: String): ArrayBuffer[CrawlerTaskEntity] = {
    val childrenTaskBuffer = new ArrayBuffer[CrawlerTaskEntity]
    try {
      var otherPages_li = hs.%("div#content div.paginator-wrapper ul.paginator li", content)
      if (otherPages_li != null && !otherPages_li.isEmpty) {
        val currentPage = otherPages_li.find { li => li.hasClass("current") }.get
        val index = otherPages_li.indexOf(currentPage)
        // current page is first list page.
        if (index == 0) {
          logInfo(s"currentPage [$index]")
          PagingListCounter += 1
          val childTaskUrlTemplate = "@?mtt=1.shops%2Fdefault.0.0.idh2s8x5"
          val lastPage = otherPages_li.last
          val lastPageA = getHeadElement(hs, "a", lastPage)
          val lastPageText = lastPageA.html()
          logInfo(s"lastPageText [$lastPageText]")
          if (StringUtils.isNotBlank(lastPageText)) {
            if (lastPageText.contains("尾页")) {
              //  totalPageNum > 7
              val lastPageHref = getElementAttrWrapped(hs, "a", lastPage, "href")
              logInfo(s"lastPageHref [$lastPageHref]")
              val idx = lastPageHref.indexOf("/page")
              if (idx > 0) {
                val totalPageNum = lastPageHref.substring(idx + 5).toInt
                logInfo(s"totalPageNum [$totalPageNum]")
                val pageLinkUrlPrefix = lastPageHref.replace("" + totalPageNum, "")
                for (p <- 2 to totalPageNum) {
                  val task = taskEntity.geneSiblingTaskEntity()
                  task.taskURI = childTaskUrlTemplate.replace("@", pageLinkUrlPrefix + p)
                  PagingListCounter += 1
                  logInfo(s"getPagingListTaskEntity [$PagingListCounter]" + task.taskURI)
                  childrenTaskBuffer += task
                }
              }
            } else {
              // totalPageNum <= 7
              otherPages_li = otherPages_li.tail.reverse.tail.reverse
              otherPages_li.foreach { page_li =>
                {
                  val pageHref = getElementAttrWrapped(hs, "a", page_li, "href")
                  if (StringUtils.isNotBlank(pageHref)) {
                    val task = taskEntity.geneSiblingTaskEntity()
                    task.taskURI = pageHref + "?mtt=1.shops%2Fdefault.0.0.idh2s8x5"
                    PagingListCounter += 1
                    logInfo(s"getPagingListTaskEntity [$PagingListCounter]" + task.taskURI)
                    childrenTaskBuffer += task
                  }
                }
              }
            }
          }
        }
      }
    } catch {
      case t: Throwable => logError("getPagingListTaskEntity error", t)
    }
    childrenTaskBuffer
  }

  // city district trade-area merchants list page
  var merchantCounterB = 0
  override def geneNewTaskEntitySet4(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    try {
      val taskEntity = resObj.tastEntity
      val response: Response = resObj.response
      val content = Util.getResponseContent(response)
      merchantCounterB += 1
      logInfo(s"geneNewTaskEntitySet4  [$merchantCounterB] " + taskEntity.taskURI)
      val foodMerchant = new FoodMerchant
      foodMerchant.setJobId(taskEntity.jobId)
      foodMerchant.setJobName(taskEntity.jobName)
      foodMerchant.setTaskId(taskEntity.taskId)
      foodMerchant.setOriginSite(taskEntity.taskDomain)
      foodMerchant.setUrl(taskEntity.taskURI)
      foodMerchant.setBatchId(taskEntity.currentBatchId)

      val om: ObjectMapper = new ObjectMapper()
      val body = hs.%(content).headOption.getOrElse(null)
      if (body != null) {
        val map_canvas_span = getHeadElement(hs, "span#map-canvas", body)
        if (map_canvas_span != null) {
          val mapDataJsonStr = map_canvas_span.attr("data-params")
          getMerchantInfoFromJsonData(foodMerchant, mapDataJsonStr, om)
        }
        val city_aLink = getHeadElement(hs, "#site-mast.site-mast div.city-info a.city-info__name", body)
        if (city_aLink != null) {
          val cityName = city_aLink.text().trim().split(" ").headOption.getOrElse("")
          foodMerchant.setCity(cityName)
        }
        val address_span = getHeadElement(hs, "div#bd p.under-title span.geo", body)
        if (address_span != null) {
          val address = address_span.text().trim()
          foodMerchant.setAddress(address)
        }
        val rankScore_strong = getHeadElement(hs, "div#bd.cf div.info div span.biz-level strong", body)
        if (rankScore_strong != null) {
          val rankScore = rankScore_strong.text().trim()
          foodMerchant.setRankScore(rankScore)
        }
        val customerCount_span = getHeadElement(hs, "div#bd.cf div.counts div span.num", body)
        if (customerCount_span != null) {
          val customerCount = customerCount_span.text().trim()
          foodMerchant.setCustomerCount(customerCount)
        }

        val commentCount_a = getHeadElement(hs, "div#bd.cf div.counts div a.num.rate-count", body)
        if (commentCount_a != null) {
          val commentCount = commentCount_a.text().trim()
          foodMerchant.setCommentCount(commentCount)
        }
        val shop_identity_info_i = getHeadElement(hs, "div#bd div.counts div.shop-identity-wrapper a.shop-identity-link i.shop-identity", body)
        if (shop_identity_info_i != null) {
          val certificateDataJsonStr = shop_identity_info_i.attr("data-certificate")
          getMerchantIdentifyInfoFromJsonData(foodMerchant, certificateDataJsonStr, om)
        }
        getMerchantInfoDetail(foodMerchant, body)
        getMerchantMenu(foodMerchant, body)
        
        val ratelist_content_div = getHeadElement(hs, "div#evaluate-box div.ratelist-content.cf", body)
        if (ratelist_content_div != null) {
          val ratelist_ul = getHeadElement(hs, "ul.J-rate-list", ratelist_content_div)
          //val 
        }
        //getMerchantComments()
      }
      logInfo(foodMerchant.toString())
      (null, (Array(foodMerchant), false))
    } catch {
      case t: Throwable => {
        logError("geneNewTaskEntitySet4 error", t)
        (null, null)
      }
    }
  }

  private def getMerchantMenu(foodMerchant: FoodMerchant, body: Element) {
    try {
      val meishi_menu_div = getHeadElement(hs, "div#meishi-menu", body)
      if (meishi_menu_div != null) {
        val title_h2 = getHeadElement(hs, "h2.content-title", meishi_menu_div)
        if (title_h2 != null) {
          val menuTitle = title_h2.text().trim()
          val menu = new SpecialityMenu
          val menus = new java.util.ArrayList[Menu]
          menus.add(menu)
          foodMerchant.setMenus(menus)
          menu.setMenuName(menuTitle)
          menu.setBatchId(foodMerchant.getBatchId)
          menu.setJobId(foodMerchant.getJobId)
          menu.setJobName(foodMerchant.getJobName)
          menu.setOriginSite(foodMerchant.getOriginSite)
          menu.setTaskId(foodMerchant.getTaskId)
          menu.setMerchantId(foodMerchant.getObjectId)
          getMerchantFoods(menu, meishi_menu_div)
        }
      }
    } catch {
      case t: Throwable => logError("getMerchantFoodList error", t)
    }
  }

  private def getMerchantFoods(menu: SpecialityMenu, meishi_menu_div: Element) {
    try {
      val foods_table = hs.%("div.menu__items table", meishi_menu_div)
      if (foods_table != null && !foods_table.isEmpty) {
        val tbOption = foods_table.find { table => { table.hasClass("hidden") } }
        val tb = tbOption.getOrElse(foods_table.head)
        val tdList = hs.%("td", tb)
        if (tdList != null && !tdList.isEmpty) {
          val foods = new java.util.ArrayList[Food]
          menu.setFoods(foods)
          for (td <- tdList) {
            val foodDesc = td.text()
            if (StringUtils.isNotBlank(foodDesc)) {
              val food = new SpecialityFood
              food.setBatchId(menu.getBatchId)
              food.setJobId(menu.getJobId)
              food.setJobName(menu.getJobName)
              food.setTaskId(menu.getTaskId)
              food.setOriginSite(menu.getOriginSite)
              food.setMenuId(menu.getObjectId)
              val temp = foodDesc.split("\\s", 2)
              food.setFoodName(temp(0))
              if (temp.length > 1) {
                food.setPrice(temp(1).replaceAll("[()]", ""))
              }
              foods.add(food)
            }
          }
        }
      }
    } catch {
      case t: Throwable => logError("getMerchantFoods error", t)
    }
  }

  private def getMerchantInfoDetail(foodMerchant: FoodMerchant, body: Element) {
    try {
      val logoImg_img = getHeadElement(hs, "div#content div.cf span.img-wrapper img", body)
      if (logoImg_img != null) {
        val logoUrl = logoImg_img.attr("src").trim()
        foodMerchant.setLogoUrl(logoUrl)
      }
      val merchantInfoDetail_divs = hs.%("div#content div.poi-section div.cf div.field-group", body)
      if (merchantInfoDetail_divs != null && !merchantInfoDetail_divs.isEmpty) {
        merchantInfoDetail_divs.foreach { field_group_div =>
          {
            if (field_group_div != null) {
              val field_title_span = getHeadElement(hs, "span.field-title", field_group_div)
              if (field_title_span != null) {
                val field_title = field_title_span.text().trim()
                if (field_title.contains("营业时间：")) {
                  val businessHoursDesc = field_group_div.text()
                  if (StringUtils.isNotBlank(businessHoursDesc)) {
                    val businessHours = businessHoursDesc.replace("营业时间：", "").trim()
                    foodMerchant.setBusinessHours(businessHours)
                  }
                }

                if (field_title.contains("门店风格")) {
                  val merchantStyleDesc_span = getHeadElement(hs, "span.inline-item", field_group_div)
                  if (merchantStyleDesc_span != null) {
                    val merchantStyleDesc = merchantStyleDesc_span.text()
                    if (StringUtils.isNotBlank(merchantStyleDesc)) {
                      foodMerchant.setMerchantStyle(merchantStyleDesc.trim())
                    }
                  }
                }

                if (field_title.contains("门店服务")) {
                  val extraServices_spans = hs.%("span.inline-item", field_group_div)
                  if (extraServices_spans != null && !extraServices_spans.isEmpty) {
                    val extraServicesList = for (extraServices_span <- extraServices_spans) yield {
                      val extraServicesDesc = extraServices_span.text()
                      if (StringUtils.isNotBlank(extraServicesDesc)) {
                        extraServicesDesc.trim()
                      }
                    }
                    if (extraServicesList != null && !extraServicesList.isEmpty) {
                      foodMerchant.setExtraServices(extraServicesList.mkString(" "))
                    }
                  }
                }

                if (field_title.contains("门店介绍")) {
                  val merchantDesc_span = getHeadElement(hs, "span.long-biz-info", field_group_div)
                  if (merchantDesc_span != null) {
                    val merchantDesc = merchantDesc_span.text()
                    if (StringUtils.isNotBlank(merchantDesc)) {

                      foodMerchant.setMerchantDesc(merchantDesc.replace("收起", "").trim())
                    }
                  }
                }
              }
            }
          }
        }
      }
    } catch {
      case t: Throwable => logError("getMerchantInfoDetail error", t)
    }
  }

  private def getMerchantIdentifyInfoFromJsonData(foodMerchant: FoodMerchant, certificateDataJsonStr: String, om: ObjectMapper) {
    try {
      if (StringUtils.isNotBlank(certificateDataJsonStr)) {
        val rootJN = om.readTree(certificateDataJsonStr)
        val codeJN = rootJN.get("code")
        if (codeJN != null) {
          val registrationNo = codeJN.getValueAsText.trim()
          foodMerchant.setRegistrationNo(registrationNo)
        }
        val nameJN = rootJN.get("name")
        if (nameJN != null) {
          val registrationName = nameJN.getValueAsText.trim()
          foodMerchant.setRegistrationName(registrationName)
        }
        val endDateJN = rootJN.get("endDate")
        if (endDateJN != null) {
          val endDate = endDateJN.getValueAsText.trim()
          foodMerchant.setEndDate(endDate)
        }
      }
    } catch {
      case t: Throwable => logError("getMerchantIdentifyInfoFromJsonData error", t)
    }
  }

  private def getMerchantInfoFromJsonData(foodMerchant: FoodMerchant, jsonData: String, om: ObjectMapper) {
    try {
      if (StringUtils.isNotBlank(jsonData)) {
        val rootJN = om.readTree(jsonData)
        val shotsJN = rootJN.get("shops")
        if (shotsJN != null) {
          val itFN = shotsJN.getFieldNames
          if (itFN.hasNext()) {
            val fieldName = itFN.next()
            val merchantId = fieldName.trim()
            foodMerchant.setMerchantId(merchantId)
            val shotsValueJN = shotsJN.get(fieldName)
            if (shotsValueJN != null) {
              val nameJN = shotsValueJN.get("name")
              if (nameJN != null) {
                foodMerchant.setName(StringUtils.trim(nameJN.getValueAsText))
              }
              val phoneJN = shotsValueJN.get("phone")
              if (phoneJN != null) {
                foodMerchant.setTelnum(StringUtils.trim(phoneJN.getValueAsText))
              }
              val positionJN = shotsValueJN.get("position")
              if (positionJN != null) {
                if (positionJN.isArray()) {
                  val latitudeJN = positionJN.get(0)
                  if (latitudeJN != null) {
                    foodMerchant.setLatitude(latitudeJN.getValueAsText)
                  }
                  val longitudeJN = positionJN.get(1)
                  if (longitudeJN != null) {
                    foodMerchant.setLongitude(longitudeJN.getValueAsText)
                  }
                }
              }
            }
          }
        }
      }
    } catch {
      case t: Throwable => logError("getMerchantInfoFromJsonData error", t)
    }
  }

}