package com.foofv.crawler.resource.master

import com.foofv.crawler.util.ActorLogReceive
import com.foofv.crawler.util.Logging
import akka.actor.Actor
import com.foofv.crawler.CrawlerConf
import akka.actor.ActorSystem
import com.foofv.crawler.util.AkkaUtil
import akka.actor.Props

class TestAkka extends Actor with ActorLogReceive with Logging {

   override def receiveRecordLog = {null}
}

object TestAkka{
  def main(args: Array[String]): Unit = {
        val conf = new CrawlerConf
     //val args = new ParserMasterArguments(argStrings, conf)
   // args.port = 9999
    val (actorSystem, _) = startSystemAndActor("127.0.0.1", 0, conf)
   // actorSystem.awaitTermination()
  }
  
  
  def startSystemAndActor(
      host: String,
      port: Int,
      conf: CrawlerConf): (ActorSystem, Int) = {
    val (actorSystem, boundPort) = AkkaUtil.createActorSystem("sdfe", host, port, conf = conf)
    val actor = actorSystem.actorOf(Props(classOf[Master], host, boundPort), "sdwww")
    (actorSystem, boundPort)
  }
}