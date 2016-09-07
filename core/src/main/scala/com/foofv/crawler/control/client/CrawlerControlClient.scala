package com.foofv.crawler.control.client

import java.util.Collections
import akka.actor._
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.agent.AgentMaster
import com.foofv.crawler.deploy.TransferMsg._
import com.foofv.crawler.enumeration._
import com.foofv.crawler.parse.topic.entity.{ShopId, AddressInfo}
import com.foofv.crawler.parse.worker.WorkerArguments
import com.foofv.crawler.storage.MongoStorage
import com.foofv.crawler.util.{ActorLogReceive, AkkaUtil, Logging}
import scala.collection.JavaConverters
import scala.concurrent.Await
import scala.collection.JavaConversions._
import com.foofv.crawler.parse.master.MasterArguments

private[crawler] class CrawlerControlClient(/*host: String, port: Int,*/ conf: CrawlerConf) extends Actor
with ActorLogReceive with Logging {


  override def preStart(): Unit = {
  }

  implicit val to = akka.util.Timeout.apply(3, java.util.concurrent.TimeUnit.SECONDS)

  val remoteActorSystemName = conf.get("", "crawlerControlManagerSys")
  //val remoteHost = "101.200.195.144"

  //val remoteHost = "123.57.239.78"
  val remoteHostPort = 9997
  val remoteHost = "192.168.1.72"
  //val remoteHostPort = 9997
  //val remoteHost = "192.168.10.25"
  //val remoteHost = "192.168.40.53"

  val remoteActorName = conf.get("", "CrawlerControlManager")
  val remoteAkkaUrl = "akka.tcp://%s@%s:%s/user/%s".format(remoteActorSystemName, remoteHost, remoteHostPort, remoteActorName)

  val actorFuture = context.actorSelection(remoteAkkaUrl).resolveOne
  val crawlerControlManager = Await.result(actorFuture, to.duration)

  override def receiveRecordLog = {
    case message: String => {
      logInfo(message)
    }
    case TaskJsonMsg(taskJsonMsg) => {
      crawlerControlManager.tell(TaskJsonMsg(taskJsonMsg), self)
    }
    case taskMsgMeituanTakeout: TaskMsgMeituanTakeout => {
      crawlerControlManager ! taskMsgMeituanTakeout
    }
  }

}

private[crawler] object CrawlerControlClient extends Logging {


  var actor: ActorRef = null

  def main(argStrings: Array[String]): Unit = {
    val conf = new CrawlerConf
    val args = new MasterArguments(argStrings, conf)


    //args.port = 9996
    //args.port = 9999

    args.host = "192.168.1.72"
    args.port = 9999

    val (actorSystem, _) = startSystemAndActor(args.host, args.port, conf)
    //val (actorSystem, _) = startSystemAndActor( "192.168.10.25", 10007, conf)
    actorSystem.awaitTermination()
  }

  def startSystemAndActor(
                           host: String,
                           port: Int,
                           conf: CrawlerConf): (ActorSystem, Int) = {
    val conf = new CrawlerConf
    val systemName = "crawlerControlClient"
    val actorName = "CrawlerControlClient"
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem(systemName, host, port, conf = conf)
    actor = actorSystem.actorOf(Props(classOf[CrawlerControlClient], /*host, port, */ conf), name = actorName)
    /*actor ! TaskJsonMsg("{\"jobname\":\"test_jobname\"," +
      "\"task\":{\"taskuri\":\"http://waimai.meituan.com/restaurant/136349?pos=33\"," +
      "\"cookies\":\"REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96'; w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\"," +
      "\"taskType\":0,\"totalDepth\":1,\"forbiddenCode\":\"302\",\"totalBatch\":1,\"userAgent\":\"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; 360SE)\",\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser\",\"httpRefer\":\"http://waimai.meituan.com/home/wx4g1ypnct88\"}}")
*/

    //presetTaskMeituanTakeoutMerchant(actor, size = 1000, batch = 1)
    //submitBaiduTakeoutMerchantTask(actor)
    //shemeSubmit(actor)
    //presetTaskMeituanTakeoutMerchant(actor, size = 200, batchSize = 50, startIndex = 20001)
    //submitBaiduTakeoutMerchantTask(actor)
    //shemeSubmit(actor)
    //actor ! TaskMsgMeituanTakeout(size = 100, batchSize = 10, startIndex = 31)
    //submitMeituanFoodMerchantTask(actor)
    //submitDianpingCusineCategoryTask(actor)
    submitDianpingRegionTask(actor)
    (actorSystem, boundPort)
  }

  def submitBaiduTakeoutMerchantsTask(actor: ActorRef, submitNumber: Int = 20, batch: Int = 1, jobName: String = "baidu:shops") {

    val jobJsonTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val jobJsonStr = jobJsonTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + System.currentTimeMillis())
    val taskTemplate = "{\"taskuri\":\"http://waimai.baidu.com/mobile/waimai?qt=shoplist&address=&lat=&lng=&page=1&display=json\",\"taskstarttime\":@taskstarttime," +
      "\"cookies\":\"\",\"userAgentType\":0,\"isUseProxy\":1," +
      "\"taskType\":0,\"totalDepth\":1,\"forbiddenCode\":\"429\",\"totalBatch\":1,\"userAgent\":\"Mozilla/5.0 (X11; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0\",\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.BaiduTakeoutEateryParser\",\"httpRefer\":\"http://waimai.baidu.com/mobile/waimai?qt=confirmcity&third_party=0\"}"

    val random = new scala.util.Random
    val batchInterval = 20000
    //    var cnt = 0
    for (i <- 0 until batch) {
      val collSize = MongoStorage.getCollectionSize(classOf[AddressInfo])
      val limit = 20
      val fetchCount = if (collSize % limit == 0) collSize / limit else collSize / limit + 1
      for (j <- 0 until fetchCount.toInt) {
        val addressInfoList = MongoStorage.getListLimited(classOf[AddressInfo], j * limit, limit)
        val taskStrSeq = for (addressInfo <- addressInfoList) yield {
          val taskStartTime = System.currentTimeMillis() + batchInterval * i + random.nextInt(/*submitNumber **/ 5000)
          val taskInfo = taskTemplate.replaceAll("address=", "address=" + addressInfo.address).
            replaceAll("lat=", "lat=" + addressInfo.latitude).
            replaceAll("lng=", "lng=" + addressInfo.longitude).
            replace("@taskstarttime", "" + taskStartTime)
          //            println("taskInfo: " + taskInfo)
          taskInfo
        }
        val taskJsonStr = "[" + taskStrSeq.mkString(",") + "]"
        val taskJsonMsg = "{" + jobJsonStr + "\"task\":" + taskJsonStr + "}"
        actor ! TaskJsonMsg(taskJsonMsg)
      }
    }
    //      cnt += 1
    //      println("count= " + cnt)
  }

  def submitBaiduTakeoutMerchantTask(actor: ActorRef, submitNumber: Int = 20, batch: Int = 1, jobName: String = "baidu:shop") {

    val jobJsonTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val jobJsonStr = jobJsonTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + System.currentTimeMillis())
    val taskTemplate = "{\"taskuri\":\"http://waimai.baidu.com/mobile/waimai?qt=shopmenu&shop_id=&display=json\",\"taskstarttime\":@taskstarttime," +
      "\"cookies\":\"\",\"userAgentType\":0,\"isUseProxy\":1," +
      "\"taskType\":0,\"totalDepth\":1,\"forbiddenCode\":\"429\",\"totalBatch\":1,\"userAgent\":\"Mozilla/5.0 (X11; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0\",\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.BaiduTakeoutEateryParser\",\"httpRefer\":\"http://waimai.baidu.com/mobile/waimai?qt=confirmcity&third_party=0\"}"

    val random = new scala.util.Random
    val batchInterval = 20000
    //    var cnt = 0
    for (i <- 0 until batch) {
      val collSize = MongoStorage.getCollectionSize(classOf[ShopId])
      val limit = 20
      val fetchCount = if (collSize % limit == 0) collSize / limit else collSize / limit + 1
      for (j <- 0 until fetchCount.toInt) {
        val shopIdList = MongoStorage.getListLimited(classOf[ShopId], j * limit, limit)
        val taskStrSeq = for (shopId <- shopIdList) yield {
          val taskStartTime = System.currentTimeMillis() + batchInterval * i + random.nextInt(/*submitNumber **/ 5000)
          val taskInfo = taskTemplate.replaceAll("shop_id=", "shop_id=" + shopId.shop_id).replace("@taskstarttime", "" + taskStartTime)
          //            println("taskInfo: " + taskInfo)
          taskInfo
        }
        val taskJsonStr = "[" + taskStrSeq.mkString(",") + "]"
        val taskJsonMsg = "{" + jobJsonStr + "\"task\":" + taskJsonStr + "}"
        actor ! TaskJsonMsg(taskJsonMsg)
      }
    }
    //      cnt += 1
    //      println("count= " + cnt)
  }

  def presetTaskMeituanTakeoutMerchant(actor: ActorRef, size: Int = 50000, startIndex: Int = 1, batchSize: Int = 500, jobName: String = "MeituanTakeoutMerchant") {
    val jobNameTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val taskTemplate = "{\"taskuri\":\"http://waimai.meituan.com/restaurant/@merchantId?pos=@pos\",\"taskstarttime\":@taskstarttime," +
      "\"cookies\":\"IJSESSIONID=zd06978gz2tw15vbq7sgezkob; iuuid=C8A2B33D0704DA7534EA03CBDD18EAB86226BF08B8C7328759823791E00ED96F; " +
      "ioc=9WR_Nseo4gUCWi2YccBKfkFPudcpBC_GtEPlykst8YtqtuxQlwH9PNPz4aq_yomYIo0e2Buqyv7N5WrcnMnh1foigiD6dO-L71Rm-sfcle8; latlng=31.221034,121.535086,1440468152180;" +
      " webp=1; ci3=1; a2h=2; abt=1440468514.0%7CBDF; rvct=10; SID=soledaroq7meep0jlj4cldocb2; ci=1; i_extend=C_b1E864817395347069952_a3279692_c0Gimthomepagecategory11H__a100173__b1; " +
      "stick-qrcode=1; ignore-zoom=true; ppos=39.920069%2C116.44267; pposn=%E5%90%8D%E5%B1%8B%E9%93%81%E6%9D%BF%E7%83%A7%E8%87%AA%E5%8A%A9%E9%A4%90; SERV=mos; " +
      "LREF=aHR0cHM6Ly9tb3MubWVpdHVhbi5jb20vdXNlcnMvc2lnbmluZw==; ttgr=355883; rvd=703926; __mta=142933642.1440468546949.1440468555820.1440492013105.4; " +
      "__utma=211559370.1150074935.1440468900.1440469534.1440491874.3; __utmb=211559370.3.9.1440492011356; __utmc=211559370; __utmz=211559370.1440468900.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); " +
      "__utmv=211559370.|3=dealtype=171=1; REMOVE_ACT_ALERT=1; w_uuid=455e1afb-d6fc-4334-b324-8aea493719d1; " +
      "uuid=f7a92229e0da16a19474.1440468514.0.0.0; oc=qH4yJAJEHAFloXFyJ1zBxYo4Yhbm3kHbK1Ij7CMptdllVU66rIEslt5HAYXc7Bua2wiVnRvEUXmsM28qKsoQB0AuuvwZgpiRJaLA9xmgGTWB6s0Dj8D37Gwr" +
      "4VCd8VVP2Cy0XS-x3q36QJFzEwpTFb-dSTgTzVaXv7wYz8wc540; w_cid=110100; w_cpy_cn=\'%E5%8C%97%E4%BA%AC\'; w_cpy=beijing; " +
      "waddrname=\'%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96\'; w_geoid=wx4g1ypnct88; " +
      "w_ah=\'39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96\'; " +
      "w_utmz=\'utm_campaign=(direct)&utm_source=(direct)&utm_medium=(none)&utm_content=(none)&utm_term=(none)\'; " +
      "w_visitid=b734e00e-e955-4277-a2ef-ae9248315710; JSESSIONID=165wko5g8p9mj1b6uksx91wy1y; __mta=142933642.1440468546949.1440492013105.1440493436387.5" +
      "\",\"taskType\":0,\"totalDepth\":1,\"forbiddenCode\":\"302\",\"totalBatch\":1," +
      "\"userAgent\":\"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; en-us) AppleWebKit/534.50 (KHTML, like Gecko) Version/5.1 Safari/534.50\"," +
      "\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.MeituanTakeoutMerchantTopicParser\"," +
      "\"proxyhost\":\"112.25.41.136\",\"proxyport\":\"80\",\"isuseproxy\":0,\"keywordsofinvalid\":\"offline\"," +
      "\"httpRefer\":\"http://waimai.meituan.com/home/wx4g1ypnct88\"}"

    val random = new scala.util.Random
    var totalSize = size
    if (totalSize < 1) {
      totalSize = 1
    }
    var sizePerBatch = batchSize
    if (sizePerBatch < 1) {
      sizePerBatch = 100
    }
    var pendingSize = totalSize
    var batchCounter = 0
    var taskStrSeq: Array[String] = Array.empty
    var counter = 0
    val batchInterval = 30000
    var GroupSize = 1000
    var groupCounter = 0
    var nextIndex = startIndex
    while (pendingSize > 0) {
      var tempMax = nextIndex
      var tempMin = nextIndex
      if (pendingSize > GroupSize) {
        tempMax += GroupSize - 1
      } else {
        tempMax += pendingSize - 1
      }
      logInfo(s"nextIndex [$nextIndex] tempMax [$tempMax]")
      val tempList = (tempMin to tempMax).toSeq.toArray
      val tempJavaList = JavaConverters.mutableSeqAsJavaListConverter(tempList).asJava
      Collections.shuffle(tempJavaList)
      var tempMerchantIdBuffer = JavaConverters.asScalaBufferConverter(tempJavaList).asScala.toArray
      for (i <- tempList) {
        val psedoPos = random.nextInt(20)
        val psedoTaskStartTime = System.currentTimeMillis() + batchInterval * batchCounter + random.nextInt(5000)
        //logInfo(s"i[$i] psedoTaskStartTime " + (psedoTaskStartTime - System.currentTimeMillis()))
        //logInfo("psedoMerchantId:" + i)
        val taskStr = taskTemplate.replace("@merchantId", "" + i).replace("@pos", "" + psedoPos).replace("@taskstarttime", "" + psedoTaskStartTime)
        taskStrSeq = taskStrSeq.+:(taskStr)
        if (counter % sizePerBatch == 0 || counter == totalSize) {
          val jobNameJsonStr = jobNameTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + batchCounter)
          logInfo("before drop taskStrSeq.length  " + taskStrSeq.length)
          logInfo(s"batchCounter [$batchCounter] ")
          val taskJsonStr = "[" + taskStrSeq.mkString(",") + "]"
          val taskJsonMsg = "{" + jobNameJsonStr + "\"task\":" + taskJsonStr + "}"
          logInfo(s"taskJsonMsg [$taskJsonMsg]")
          batchCounter += 1
          taskStrSeq = taskStrSeq.drop(counter)
          logInfo("taskStrSeq.length  " + taskStrSeq.length)
          actor ! TaskJsonMsg(taskJsonMsg)
          Thread.sleep(5000)
        }
        counter += 1
      }
      logInfo(s"tempJavaList [$tempJavaList] ")
      nextIndex = tempMax + 1
      pendingSize -= GroupSize
      groupCounter += 1
    }
    logInfo(s"\n\tstatistics: \ntotalSize[$totalSize], \nGroupSize[$GroupSize], groupCounter[$groupCounter], \nbatchSize[$batchSize], batchCounter[$batchCounter], \n  counter[$counter]")
    /*
    val tempList = (1 to totalSize).toSeq.toArray
    val tempJavaList = JavaConverters.mutableSeqAsJavaListConverter(tempList).asJava
    Collections.shuffle(tempJavaList)
    var merchantIdBuffer = JavaConverters.asScalaBufferConverter(tempJavaList).asScala.toArray
    val averageBatchSize = if (size / 100 > 0) size / 100 else 1
    var nextStartIndex = 0
    var batchInterval = 20000
    for (i <- 0 until 100 if nextStartIndex < size) {
      var count = averageBatchSize + random.nextInt(averageBatchSize) / 2
      if (count >= size - nextStartIndex) {
        count = size - nextStartIndex
      }
      logInfo("count:" + count)
      logInfo("batch :" + (i + 1))
      var tempArray = new Array[Int](count)
      merchantIdBuffer.copyToArray(tempArray, 0, count)
      merchantIdBuffer = merchantIdBuffer.drop(count)
      nextStartIndex = nextStartIndex + count
      logInfo("nextStartIndex:" + nextStartIndex)
      //logInfo("tempArray: " + tempArray.mkString("[",",","]"))
      val taskStrSeq = for (psedoMerchantId <- tempArray) yield {
        val psedoPos = random.nextInt(20)
        val psedoTaskStartTime = System.currentTimeMillis() + batchInterval * i + random.nextInt(5000)
        logInfo("psedoMerchantId:" + psedoMerchantId)
        logInfo("" + (psedoTaskStartTime - System.currentTimeMillis()))
        val taskStr = taskTemplate.replace("@merchantId", "" + psedoMerchantId).replace("@pos", "" + psedoPos).replace("@taskstarttime", "" + psedoTaskStartTime)
        taskStr
      }
      val taskJsonStr = "[" + taskStrSeq.mkString(",") + "]"
      val taskJsonMsg = "{" + jobNameJsonStr + "\"task\":" + taskJsonStr + "}"
      actor ! TaskJsonMsg(taskJsonMsg)
    }
*/
  }

  def shemeSubmit(actor: ActorRef, jobName: String = "dazhongdianping_shceme") {
    val jobNameTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val jobNameJsonStr = jobNameTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + "1234567890")
    val taskTemplate = "{\"taskType\":0,\"totalBatch\":1," +
      "\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.SchemeTopicParser\"," +
      "\"userAgent\":\"User-Agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 LBBROWSER\"," +
      "\"cookies\":\"showNav=#nav-tab|0|0; PHOENIX_ID=0a651016-15003a13b6d-d6069; _hc.v=\'298a09e3-2720-4b2b-ba4d-d0fc2f3a13dc.1443169909\'; __utma=205923334.125818291.1443169913.1443169913.1443169913.1; __utmb=205923334.1.10.1443169913; __utmc=205923334; __utmz=205923334.1443169913.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); s_ViewType=10; JSESSIONID=B8F3E8A50CB9FDB0B617981FCC66F25C; aburl=1; cy=2; cye=beijing\"," +
      "\"schemeFile\":\"d:\\\\workspace-sts-3.6.2.RELEASE\\\\crawler-fetch\\\\core\\\\src\\\\main\\\\resources\\\\schema_example\\\\DemoDazhongdianping.xml\"}"

    val taskJsonMsg = "{" + jobNameJsonStr + "\"task\":" + taskTemplate + "}"
    actor ! TaskJsonMsg(taskJsonMsg)
  }

  def submitMeituanFoodMerchantTask(actor: ActorRef, jobName: String = "MeituanFoodMerchantTask") {
    val jobNameTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val jobNameJsonStr = jobNameTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + System.currentTimeMillis())
    val taskTemplate = "{\"taskuri\":\"http://www.meituan.com/index/changecity/initiative?mtt=1.index%2Ffloornew.0.0.idgs0y4o\",\"taskType\":0,\"totalBatch\":1,\"totalDepth\":4," +
      "\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.MeituanFoodMerchantParser\"," +
      "\"userAgent\":\"User-Agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 LBBROWSER\"," +
      "\"cookies\":\"REMOVE_ACT_ALERT=1; w_uuid=d296f0c5-0391-415e-aa29-d1273df1aff7; _gat=1; w_cid=110100; " +
      "w_cpy_cn='%E5%8C%97%E4%BA%AC'; w_cpy=beijing; waddrname='%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96';" +
      " w_geoid=wx4g1ypnct88; w_ah='39.9364909529686,116.45376987755299,%E5%B7%A5%E4%BD%93%2C%E4%B8%89%E9%87%8C%E5%B1%AF%2C%E6%9C%9D%E5%A4%96|31.24579595401883,121.52601286768913,%E6%B5%A6%E4%B8%9C%E5%A4%A7%E9%81%93'; " +
      "w_utmz='utm_campaign=baidu&utm_source=4204&utm_medium=(none)&utm_content=(none)&utm_term=(none)'; w_visitid=bc84a5bb-63d1-433c-b018-2ce2e217b4c9; " +
      "JSESSIONID=19g6ebjbt46j9c8u4ipp3jwc8; _ga=GA1.3.2013913880.1438047460; __mta=141295501.1438047460706.1438062027541.1438062048624.20\"}"

    val taskJsonMsg = "{" + jobNameJsonStr + "\"task\":" + taskTemplate + "}"
    actor ! TaskJsonMsg(taskJsonMsg)
  }

  def submitDianpingCityTask(actor: ActorRef, jobName: String = "DianpingCityTask") {
    val jobNameTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val jobNameJsonStr = jobNameTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + "1234567890")
    val taskTemplate = "{\"taskType\":0,\"totalBatch\":1," +
      "\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.SchemeTopicParser\"," +
      "\"userAgent\":\"User-Agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 LBBROWSER\"," +
      "\"cookies\":\"_hc.v=\'05d3a3e0-78a2-4dde-a801-1011169ac22e.1439886641\';" +
      " PHOENIX_ID=0a017626-14faaa6f348-eaba0e; __utma=1.58018458.1441621883.1441621883.1441676807.2;" +
      " __utmc=1; __utmz=1.1441621883.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); s_ViewType=10; " +
      "JSESSIONID=AAE53EF5FEA86445BCE52E83C5A81B2D; aburl=1; cy=3317; cye=delhi\"," +
      "\"schemeFile\":\"core/src/main/resources/dianping-city.xml\"}"

    val taskJsonMsg = "{" + jobNameJsonStr + "\"task\":" + taskTemplate + "}"
    actor ! TaskJsonMsg(taskJsonMsg)
  }

  def submitDianpingCusineCategoryTask(actor: ActorRef, jobName: String = "DianpingCusineCategoryTask") {
    val jobNameTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val jobNameJsonStr = jobNameTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + "12345678")
    val taskTemplate = "{\"taskType\":0,\"totalBatch\":1," +
      "\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.SchemeTopicParser\"," +
      "\"userAgent\":\"User-Agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 LBBROWSER\"," +
      "\"cookies\":\"_hc.v=\'05d3a3e0-78a2-4dde-a801-1011169ac22e.1439886641\';" +
      " PHOENIX_ID=0a017626-14faaa6f348-eaba0e; __utma=1.58018458.1441621883.1441621883.1441676807.2;" +
      " __utmc=1; __utmz=1.1441621883.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); s_ViewType=10; " +
      "JSESSIONID=AAE53EF5FEA86445BCE52E83C5A81B2D; aburl=1; cy=3317; cye=delhi\"," +
      "\"schemeFile\":\"src/main/resources/dianping-cusine-category.xml\"}"

    val taskJsonMsg = "{" + jobNameJsonStr + "\"task\":" + taskTemplate + "}"
    actor ! TaskJsonMsg(taskJsonMsg)
  }
  
  ///home/crawler/project-crawler-fetch/core/
  def submitDianpingRegionTask(actor: ActorRef, jobName: String = "DianpingRegionTask") {
    val jobNameTemplate = "\"jobname\":\"@jobname\",\"jobid\":@jobid,"
    val jobNameJsonStr = jobNameTemplate.replaceFirst("@jobname", jobName).replace("@jobid", "" + "123456789")
    val taskTemplate = "{\"taskType\":0,\"totalBatch\":1," +
      "\"topciCrawlerParserClassName\":\"com.foofv.crawler.parse.topic.SchemeTopicParser\"," +
      "\"userAgent\":\"User-Agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 LBBROWSER\"," +
      "\"cookies\":\"navCtgScroll=19; showNav=#nav-tab|0|1; _hc.v=\'a0ab31e4-0f98-432c-a878-0c77d0e2f49d.1443169414\'; __utma=1.2007844807.1443172774.1443496210.1443509743.4; __utmz=1.1443172774.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); PHOENIX_ID=0a017918-1504543bfe7-11d4ed9; s_ViewType=10; JSESSIONID=EEBC7E5E490EEFD0CCFE98402206BE31; aburl=1; cy=2; cye=beijing\"," +
      "\"schemeFile\":\"core/src/main/resources/dianping-city-region.xml\"}"

    val taskJsonMsg = "{" + jobNameJsonStr + "\"task\":" + taskTemplate + "}"
    actor ! TaskJsonMsg(taskJsonMsg)
  }

}

