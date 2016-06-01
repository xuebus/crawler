package com.foofv.crawler.parse.topic

import java.security.MessageDigest
import java.text.NumberFormat
import java.util.concurrent.ConcurrentHashMap

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.blockqueue.CrawlerBlockQueue
import com.foofv.crawler.entity.{CrawlerTaskEntity, ResObj}
import com.foofv.crawler.parse.{Parser, TopicParser}
import com.foofv.crawler.parse.topic.entity._
import com.foofv.crawler.parse.xpath.JsonMonster
import com.foofv.crawler.storage.MongoStorage
import com.foofv.crawler.util.Util
import org.apache.hadoop.hdfs.DFSClient.Conf
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
 * Created by msfenn on 27/07/15.
 */
class BaiduTakeoutEateryParser extends TopicParser {

  val md5 = MessageDigest.getInstance("MD5")
  val parserAndObjAsynCacheQueue = CrawlerBlockQueue[(Parser, ResObj, AnyRef)]("linkedBlock", "asynParserObj", Int.MaxValue, new CrawlerConf())
  val propertiesOfPage1 = Array("deliveryStartPrice", "sendPrice", "soldTotal", "averageSendTime", "name", "merchantId", "logoUrl", "latitude", "longitude"
    , "address", "rankScore", "isOffShelf", "merchantCategoryDimension", "discountCompaign", "extraServices", "invoiceSupported"
    , "onlinepaySupported", "originSite", "url", "taskId", "batchId", "jobId", "jobName", "objectId")
  val propertiesOfPage2 = Array("menus", "telnum", "cityId", "address", "businessHours")

  def getExistingTakeoutEatery(merchantId: String): TakeoutMerchant = {

    val array = parserAndObjAsynCacheQueue.toArray()
    if (array == null)
      return null
    val triples = array.asInstanceOf[Array[(Parser, ResObj, AnyRef)]]
    for (triple <- triples) {
      if (triple._1 == this) {
        for (takeoutEatery <- triple._3.asInstanceOf[Seq[TakeoutMerchant]]) {
          if (takeoutEatery.getMerchantId.equals(merchantId))
            return takeoutEatery
        }
      }
    }
    return null
  }

  /**
   * Generate sub-tasks from the 1st-level crawling
   * @param resObj
   * @return (Seq[CrawlerTaskEntity], AnyRef) : tuple consisting of sub-tasks and parsed entity(list of TakeoutMerchant)
   */
  override protected def geneNewTaskEntitySet1(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {

    if (!resObj.tastEntity.jobName.contains("shops"))
      return (null, null)
    //val shopURL = "http://waimai.baidu.com/mobile/waimai?qt=shopmenu&shop_id=0&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&display=json"
    val shopURL = resObj.tastEntity.taskURI
    //    val takeoutEateryList = new ListBuffer[TakeoutMerchant]()
    var taskEntityList: ListBuffer[CrawlerTaskEntity] = new ListBuffer[CrawlerTaskEntity]()
    //    val taskEntity = resObj.tastEntity
    val jsonContent = Util.getResponseContent(resObj.response)
    val jsonParser = new JsonMonster()
    val shopInfoList = jsonParser.getValuesFromJson(jsonContent, "result.shop_info")
    val shops = jsonParser.getValueFromJson(jsonContent, "total").toInt
    //    var taskOrdinal = 1
    //    for (shopInfo <- shopInfoList) {
    //      val merchantId = jsonParser.getValueFromJson(shopInfo, "shop_id")
    //      val childTaskEntity = resObj.tastEntity.geneChildTaskEntity()
    //      childTaskEntity.taskStartTime = System.currentTimeMillis() + getChildTaskIntervalTime
    //      childTaskEntity.taskURI = shopURL.replaceAll("qt=(\\w+)", "qt=shopmenu&shop_id=" + merchantId)
    //      childTaskEntity.taskOrdinal = taskOrdinal
    //      taskEntityList += childTaskEntity
    //      taskOrdinal += 1
    //    }

    if (shopURL.contains("&page=1&")) {
      val requests = shops / shopInfoList.size + {
        if (shops % shopInfoList.size == 0) 0 else 1
      }
      //      if (requests >= 2)
      //        taskEntityList = new ListBuffer[CrawlerTaskEntity]()
      for (i <- 2 to requests) {
        val siblingTaskEntity = resObj.tastEntity.geneSiblingTaskEntity()
        siblingTaskEntity.taskStartTime = System.currentTimeMillis() + getChildTaskIntervalTime
        siblingTaskEntity.taskURI = shopURL.replaceAll("page=(\\d+)", "page=" + i)
        siblingTaskEntity.taskOrdinal = i
        siblingTaskEntity.currentDepthCompleted = "false"
        taskEntityList += siblingTaskEntity
      }
    }

    //    depthEntityMap.put(resObj.tastEntity.currentDepth, takeoutEateryList.toList)
    //
    //    val values = Array.ofDim[AnyRef](takeoutEateryList.length, propertiesOfPage1.length)
    //    for (i <- 0 until takeoutEateryList.length) {
    //
    //    }
    //
    //    val returnValue = (classOf[TakeoutMerchant], takeoutEateryList.map(item => item.getObjectId).toArray, propertiesOfPage1, )
    (taskEntityList.toSeq /*Seq(taskEntityList(0))*/ , null)
  }


  override protected def parse1DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {

    if (resObj.tastEntity.jobName.contains("address-coordinate")) {
      //coordinate request result parsing
      val jsonContent = Util.getResponseContent(resObj.response)
      val jsonParser = new JsonMonster()
      val coordinateList = jsonParser.getValuesFromJson(jsonContent, "result.content")
      var latitude = ""
      var longitude = ""
      (0 /: coordinateList) {
        (shops, coordinateInfo) => {
          val shopNumber = jsonParser.getValueFromJson(coordinateInfo, "shopnum").toInt
          if (shopNumber > shops) {
            latitude = jsonParser.getValueFromJson(coordinateInfo, "latitude")
            longitude = jsonParser.getValueFromJson(coordinateInfo, "longitude")
            shopNumber
          } else {
            shops
          }
        }
      }

      if (coordinateList.size > 0 && latitude.length > 0) {
        val addressInfo = new AddressInfo()
        val regex = ".+wd=(.+)".r
        val regex(address) = resObj.tastEntity.taskURI
        val numberFormat = NumberFormat.getInstance()
        numberFormat.setMaximumFractionDigits(5)
        numberFormat.setGroupingUsed(false)
        addressInfo.address = address
        addressInfo.latitude = numberFormat.format(latitude.toDouble)
        addressInfo.longitude = numberFormat.format(longitude.toDouble)

        addressInfo.setBatchId(resObj.tastEntity.currentBatchId)
        addressInfo.setJobId(resObj.tastEntity.jobId)
        addressInfo.setJobName(resObj.tastEntity.jobName)
        addressInfo.setOriginSite(resObj.tastEntity.taskDomain)
        addressInfo.setTaskId(resObj.tastEntity.taskId)

        (Array(addressInfo), true)
      } else
        null
    } else if (resObj.tastEntity.jobName.contains("shops")) {
      //baidu shop list request result parsing
      val jsonContent = Util.getResponseContent(resObj.response)
      val jsonParser = new JsonMonster()
      val shopInfoList = jsonParser.getValuesFromJson(jsonContent, "result.shop_info")
      val array = new ArrayBuffer[ShopId]()
      for (shopInfo <- shopInfoList) {
        val merchantId = jsonParser.getValueFromJson(shopInfo, "shop_id")
        array.append(new ShopId(merchantId))
      }
      array
    } else {
      parse2DepthPage(resObj)
    }
  }

  override protected def parse2DepthPage(resObj: ResObj): AnyRef = {

    val takeoutMenuList = new ListBuffer[Menu]()
    val takeoutFoodList = new ListBuffer[Food]()
    val jsonContent = Util.getResponseContent(resObj.response)
    val jsonParser = new JsonMonster()
    //    val takeoutEateryList = depthEntityMap.get(resObj.tastEntity.currentDepth - 1)
    //    val takeoutEateries = parserAndObjAsynCacheQueue.toArray().asInstanceOf[Array[(Parser, ResObj, AnyRef)]]
    val takeoutEatery: TakeoutMerchant = new TakeoutMerchant //null
    val merchantId = jsonParser.getValueFromJson(jsonContent, "result.shop_id")
    takeoutEatery.setMerchantId(merchantId)
    var businessTime = jsonParser.getValueFromJson(jsonContent, "result.bussiness_hours.start")
    businessTime += "-" + jsonParser.getValueFromJson(jsonContent, "result.bussiness_hours.end")
    takeoutEatery.setBusinessHours(businessTime)
    takeoutEatery.setAddress(jsonParser.getValueFromJson(jsonContent, "result.address"))
    takeoutEatery.setCityId(jsonParser.getValueFromJson(jsonContent, "result.city_id"))
    takeoutEatery.setTelnum(jsonParser.getValueFromJson(jsonContent, "result.shop_phone"))
    takeoutEatery.setDeliveryStartPrice(jsonParser.getValueFromJson(jsonContent, "result.takeout_price"))
    takeoutEatery.setSendPrice(jsonParser.getValueFromJson(jsonContent, "result.takeout_cost"))
    takeoutEatery.setSoldTotal(jsonParser.getValueFromJson(jsonContent, "result.saled" /*"saled_month"*/))
    takeoutEatery.setAverageSendTime(jsonParser.getValueFromJson(jsonContent, "result.delivery_time"))
    takeoutEatery.setName(jsonParser.getValueFromJson(jsonContent, "result.shop_name"))
    takeoutEatery.setLogoUrl(jsonParser.getValueFromJson(jsonContent, "result.logo_url"))
    takeoutEatery.setLatitude(jsonParser.getValueFromJson(jsonContent, "result.shop_lat"))
    takeoutEatery.setLongitude(jsonParser.getValueFromJson(jsonContent, "result.shop_lng"))
    takeoutEatery.setAddress(jsonParser.getValueFromJson(jsonContent, "result.address"))
    takeoutEatery.setRankScore(jsonParser.getValueFromJson(jsonContent, "result.average_score"))
    takeoutEatery.setIsOffShelf(jsonParser.getValueFromJson(jsonContent, "result.is_online"))
    takeoutEatery.setMerchantCategoryDimension(jsonParser.getValueFromJson(jsonContent, "result.category"))

    takeoutEatery.setDiscountCompaign(jsonParser.getValueFromJson(jsonContent, "result.discount_info"))
    takeoutEatery.setExtraServices(jsonParser.getValueFromJson(jsonContent, "result.welfare_info"))
    takeoutEatery.setInvoiceSupported(jsonParser.getValueFromJson(jsonContent, "result.invoice_info.is_support_invoice"))
    for (info <- jsonParser.getValuesFromJson(jsonContent, "result.welfare_basic_info")) {
      if (info.contains("pay"))
        takeoutEatery.setOnlinepaySupported(jsonParser.getValueFromJson(info, "msg"))
    }

    takeoutEatery.setOriginSite(resObj.tastEntity.taskDomain)
    takeoutEatery.setUrl(resObj.tastEntity.taskURI)
    takeoutEatery.setTaskId(resObj.tastEntity.taskId)
    takeoutEatery.setBatchId(resObj.tastEntity.currentBatchId)
    takeoutEatery.setJobId(resObj.tastEntity.jobId)
    takeoutEatery.setJobName(resObj.tastEntity.jobName)

    var objectId = takeoutEatery.getOriginSite + "-" + getMD5(takeoutEatery.getMerchantId) + "-" + takeoutEatery.getBatchId
    takeoutEatery.setObjectId(objectId)

    val menuList = jsonParser.getValuesFromJson(jsonContent, "result.takeout_menu")
    for (menu <- menuList) {
      val takeoutMenu = new TakeoutMenu()
      takeoutMenu.setBatchId(resObj.tastEntity.currentBatchId)
      takeoutMenu.setJobId(resObj.tastEntity.jobId)
      takeoutMenu.setJobName(resObj.tastEntity.jobName)
      takeoutMenu.setTaskId(resObj.tastEntity.taskId)
      takeoutMenu.setOriginSite(resObj.tastEntity.taskDomain)

      takeoutMenu.setMerchantId(merchantId) //TODO: objectId?
      takeoutMenu.setMenuName(jsonParser.getValueFromJson(menu, "catalog", true))
      takeoutMenu.setSiteMenuId(jsonParser.getValueFromJson(menu, "category_id", true))


      //      val matchedTakeoutEateryList = takeoutEateryList.filter(_.getMerchantId.equals(takeoutMenu.getMerchantId))
      //      if (matchedTakeoutEateryList.size != 1)
      //        return null
      //      takeoutEatery = matchedTakeoutEateryList(0)

      //      takeoutEatery = getExistingTakeoutEatery(takeoutMenu.getMerchantId)
      //      if (takeoutEatery == null) {
      //        println("\t no merchantId: " + takeoutMenu.getMerchantId)
      //        takeoutEatery = new TakeoutMerchant
      //      }

      objectId = takeoutEatery.getObjectId + "/" + getMD5(takeoutMenu.getSiteMenuId) + "-" + takeoutMenu.getBatchId
      takeoutMenu.setObjectId(objectId)

      for (foodData <- jsonParser.getValuesFromJson(menu, "data")) {
        val takeoutFood = new TakeoutFood()
        takeoutFood.setBatchId(resObj.tastEntity.currentBatchId)
        takeoutFood.setJobId(resObj.tastEntity.jobId)
        takeoutFood.setJobName(resObj.tastEntity.jobName)
        takeoutFood.setTaskId(resObj.tastEntity.taskId)
        takeoutFood.setOriginSite(resObj.tastEntity.taskDomain)

        takeoutFood.setFoodDescription(jsonParser.getValueFromJson(foodData, "description"))
        takeoutFood.setFoodId(jsonParser.getValueFromJson(foodData, "item_id"))
        takeoutFood.setFoodName(jsonParser.getValueFromJson(foodData, "name"))
        takeoutFood.setImgUrl(jsonParser.getValueFromJson(foodData, "url"))
        takeoutFood.setMenuId(jsonParser.getValueFromJson(foodData, "category_id", true)) //takeoutMenu.getSiteMenuId
        takeoutFood.setOnSale(jsonParser.getValueFromJson(foodData, "on_sale"))
        takeoutFood.setPrice(jsonParser.getValueFromJson(foodData, "current_price"))
        takeoutFood.setSoldCount(jsonParser.getValueFromJson(foodData, "saled"))

        objectId = takeoutEatery.getObjectId + "//" + getMD5(takeoutFood.getFoodId) + "-" + takeoutFood.getBatchId
        takeoutFood.setObjectId(objectId)

        takeoutFoodList += takeoutFood
      }
      takeoutMenu.setFoods(takeoutFoodList.toList.asJava)
      takeoutFoodList.clear()
      takeoutMenuList += takeoutMenu
    }

    takeoutEatery.setMenus(takeoutMenuList.toList.asJava)

    (Array(takeoutEatery), false)
  }

  def getMD5(data: String) = {
    val md5Bytes = md5.digest(data.getBytes("UTF-8"))
    ("" /: md5Bytes) {
      (str, b) => str + b.formatted("%02X")
    }
  }
}

object BaiduTakeoutEateryParserTest extends App {

  //  var shopsURL = "http://waimai.baidu.com/mobile/waimai?qt=shoplist&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&page=1&display=json"
  //  val url = "http://api.map.baidu.com/telematics/v3/geocoding?keyWord=%E5%8C%97%E4%BA%AC%E5%B8%82%E4%B8%8A%E5%9C%B0%E5%8D%81%E8%A1%97%E5%8D%81%E5%8F%B7%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&cityName=131&out_coord_type=gcj02&ak=E4805d16520de693a3fe707cdc962045"
  //  var doc = Jsoup.connect(url).ignoreContentType(true).timeout(20000).execute().body()
  //  println(doc)
  //  val ls = new BaiduTakeoutEateryParser().f(doc)
  //  println(ls.size)
  //  for (x <- ls) {
  //    println(x.getDiscountCompaign)
  //  }

  //  var shopURL = "http://waimai.baidu.com/mobile/waimai?qt=shopmenu&shop_id=1441758488&address=%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93&lat=3642344.28&lng=13528361.08&display=json"
  //  var doc = Jsoup.connect(shopURL).ignoreContentType(true).timeout(20000).execute().body()
  //  val ls = new BaiduTakeoutEateryParser().g(doc)
  //  println(ls.size)
  //  for (x <- ls) {
  //    println(x.getMenuName + "\t" + x.getMenuDescription + "\t foods: " + x.getFoods.size())
  //    for (y <- x.getFoods.asScala) {
  //      print("\t" + y.getFoodName)
  //    }
  //    println()
  //  }

  //  println(shopsURL.replaceAll("qt=(\\w+)", "qt=shopmenu&shop_id=" + 1234567))
  //  val list = List(new TakeoutMerchant, new TakeoutMerchant, new TakeoutMerchant)
  //  list.foreach(item => println(item.getObjectId))
  //  val arr = list.map(a => a.getObjectId).toArray
  //  arr.foreach(println(_))

  //  val baiduParser = new BaiduTakeoutEateryParser
  //  println(baiduParser.getMD5("length"))

  //  val regex = ".+wd=(.+)".r
  //  val regex(address) = "xy&sdf&wd=%AF%D5"
  //  println(address)
  //  regex.findAllIn("xy&sdf&wd=%AF%D5").matchData.foreach(i => println(i.group(1)))

  //  val jsonParser = new JsonMonster()
  //  val lng = jsonParser.getValueFromJson("{\"lng\":\"1234567890.12345\",\"lat\":1234567890.123 }", "lng")
  //  val lat = jsonParser.getValueFromJson("{\"lng\":\"1234567890.12345\",\"lat\":1234567890.123 }", "lat")
  //  /*.toDouble*/
  //  val nf = NumberFormat.getInstance()
  //  nf.setMaximumFractionDigits(5)
  //  nf.setGroupingUsed(false)
  //  println(lng)
  //  println(lat)
  //  println("lng:" + nf.format(lng.toDouble))
  //  println("lat:" + nf.format(lat.toDouble))
}
