package com.foofv.crawler.parse.topic

import com.foofv.crawler.parse.TopicParser
import com.foofv.crawler.entity.ResObj
import com.foofv.crawler.entity.CrawlerTaskEntity
import com.foofv.crawler.downloader.Response
import com.foofv.crawler.parse.xpath.HtmlSoldier
import com.foofv.crawler.storage.StorageManager
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.storage.StorageManagerFactory
import com.foofv.crawler.parse.topic.entity._
import com.foofv.crawler.util.Util
import org.apache.commons.lang3.StringUtils
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import org.codehaus.jackson.map.ObjectMapper
import com.sun.media.sound.AlawCodec

private[crawler] class MeituanCityListTopicParser extends TopicParser {

  private val mongo = createStorageManager

  def createStorageManager(): StorageManager = {
    StorageManager("mongo", new CrawlerConf)
  }

  override def parseYourSelf(resObj: ResObj, obj: AnyRef) = { null.asInstanceOf }

  override def geneNewTaskEntitySet3(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    val TYPE = "tradeArea"
    var tradeAreaList: Buffer[AreaDim] = null
    var childTaskList: Buffer[CrawlerTaskEntity] = null
    null
  }

  override def geneNewTaskEntitySet2(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    val TYPE = "district"
    var dataList: Buffer[AreaDim] = new ArrayBuffer[AreaDim]
    var childTaskList: Buffer[CrawlerTaskEntity] = new ArrayBuffer[CrawlerTaskEntity]
    val childTaskUrlTemplate = "http://@@.meituan.com/?mtt=1.index%2Fchangecity.0.0.icfjmx13"
    val taskEntity = resObj.tastEntity
    val response = resObj.response
    val content = Util.getResponseContent(response)
    val hs = HtmlSoldier()
    var eles = hs.%("div#bd.cf div#filter div.filter-label-list.filter-section.geo-filter-wrapper li.item:gt(0)", content)
    if (eles.length > 0) {
      val subwayItem = eles(0)
      val subwayLink = subwayItem.children().first()
      if (subwayLink.classNames().contains("subway")) {
        eles = eles.tail
      }
      
      dataList = new ArrayBuffer[AreaDim]
      childTaskList = new ArrayBuffer[CrawlerTaskEntity]
      val liItemIterator = eles.iterator
      while (liItemIterator.hasNext) {
        val item = liItemIterator.next()
        val href = hs.%("a", "href", item)
        val districtName = hs.%("a", "text", item)

        val district = new AreaDim()
        district.setType(TYPE)
        district.setTaskId(taskEntity.taskId)
        district.setBatchId(taskEntity.currentBatchId)
        district.setJobId(taskEntity.jobId)
        district.setJobName(taskEntity.jobName)
        district.setCodes_default(href.split("/").lastOption.get)
        district.setName(districtName)
        district.setOriginSite(taskEntity.taskDomain)
        district.setParent_id(taskEntity.parentTaskToParames)
        dataList += district

        val childTask = taskEntity.geneChildTaskEntity()
        // get random interval time
        childTask.taskStartTime = System.currentTimeMillis() + getChildTaskIntervalTime
        childTask.taskURI = href
        childTaskList += childTask
      }
    }
    var result1: Seq[CrawlerTaskEntity] = null
    if (childTaskList != null) {
      result1 = childTaskList.toSeq
    }
    var result2: AnyRef = null
    if (dataList != null) {
      result2 = dataList.toSeq
    }
    (result1, result2)
  }

  override def geneNewTaskEntitySet1(resObj: ResObj): (Seq[CrawlerTaskEntity], AnyRef) = {
    val TYPE = "city"
    var dataList: Buffer[AreaDim] = null
    var childTaskList: Buffer[CrawlerTaskEntity] = null
    val childTaskUrlTemplate = "http://@@.meituan.com/category/?mtt=1.index%2Ffloornew.0.0.iccw5k3k"

    val taskEntity = resObj.tastEntity
    val response = resObj.response
    val content = Util.getResponseContent(response)
    val hs = HtmlSoldier()
    val eles = hs.%("#changeCity span.province-city-select", content)
    if (eles.length > 0) {
      val cityListSpan = eles(0)
      var cityListRawData = cityListSpan.attr("data-params")
      if (!cityListRawData.contains("\"format\"")) {
        cityListRawData = cityListRawData.replaceFirst("format", "\"format\"")
      }
      if (!cityListRawData.contains("\"data\"")) {
        cityListRawData = cityListRawData.replaceFirst("data", "\"data\"")
      }
      val mapper: ObjectMapper = new ObjectMapper();
      val rootNode = mapper.readTree(cityListRawData)
      val defNode = rootNode.findValue("def")
      val iterator = defNode.getFieldNames
      dataList = new ArrayBuffer[AreaDim]
      childTaskList = new ArrayBuffer[CrawlerTaskEntity]
      var i = 0
      while (iterator.hasNext() && i < 1) {
        val fieldName = iterator.next()
        if (StringUtils.isNumeric(fieldName) == false) {
          val value = defNode.get(fieldName).getTextValue
          val city = new AreaDim()
          city.setType(TYPE)
          city.setTaskId(taskEntity.taskId)
          city.setBatchId(taskEntity.currentBatchId)
          city.setJobId(taskEntity.jobId)
          city.setJobName(taskEntity.jobName)
          city.setCodes_default(fieldName)
          city.setName(value)
          city.setOriginSite(taskEntity.taskDomain)
          dataList += city

          val childTask = taskEntity.geneChildTaskEntity()
          // get random interval time
          childTask.taskStartTime = System.currentTimeMillis() + getChildTaskIntervalTime
          childTask.taskURI = childTaskUrlTemplate.replaceFirst("(@@)", city.getCodes_default)
          childTask.parentTaskToParames = city.getObjectId
          childTaskList += childTask
          i += 1
        }
      }
    }
    var result1: Seq[CrawlerTaskEntity] = null
    if (childTaskList != null) {
      result1 = childTaskList.toSeq
    }
    var result2: AnyRef = null
    if (dataList != null) {
      result2 = dataList.toSeq
    }
    (result1, result2)
  }

  override def parse1DepthPage(resObj: ResObj, obj: AnyRef): AnyRef = {
    val cityList = obj.asInstanceOf[Seq[AreaDim]]
    cityList.map { city =>
      {
        val objId = city.getObjectId
        try {
          mongo.put[AreaDim](city)
          logDebug(s"save AreaDim [$objId] success")
        } catch {
          case t: Throwable => {
            logError(s"save AreaDim [$objId] failed", t)
          }
        }
      }
    }
    null
  }

}

object TestMeituanCityListTopicParser {

  def main(args: Array[String]): Unit = {
    test
  }

  def test = {
    val conf: CrawlerConf = new CrawlerConf
    var storageManager: StorageManager = StorageManager("hbase", conf)
    //val resObj = storageManager.getByKey[ResObj]("0_1_0c42e2cc-207e-4d68-9aaa-30c4bbcdbe61")
    val resObj = storageManager.getByKey[ResObj]("0_1_8576c1c8-3def-424e-874e-31beae670b8b")
    val parser = new MeituanCityListTopicParser
    parser.geneNewTaskEntitySet(resObj)
  }
}