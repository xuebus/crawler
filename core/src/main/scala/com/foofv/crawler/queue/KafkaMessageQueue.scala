/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foofv.crawler.queue

import java.util.Collections
import java.util.Properties
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Logging
import kafka.consumer.Consumer
import kafka.consumer.ConsumerConfig
import kafka.consumer.KafkaStream
import kafka.producer.KeyedMessage
import kafka.producer.Producer
import kafka.producer.Producer
import kafka.producer.Producer
import kafka.producer.Producer
import kafka.producer.ProducerConfig
import java.util.concurrent.LinkedBlockingQueue
import com.foofv.crawler.util.constant.Constant

/**
 * @author soledede
 */
class KafkaMessageQueue private (conf: CrawlerConf) extends MessageQueue with Logging {

  //product

  //val productTopic = conf.get("com.crawler.kafka.producter.topic", "crawler_v1")
  val productTopic = Constant(conf).PRODUCT_TOPIC
  val brokers = Constant(conf).BROKERS
  val props = new Properties()
  props.put("metadata.broker.list", brokers)
  props.put("serializer.class", conf.get("com.crawler.kafka.serializer.StringEncoder", "kafka.serializer.StringEncoder"))
  //props.put("partitioner.class", conf.get("com.crawler.kafka.partitioner.class", "com.crawler.kafka.SimplePartitioner"))
  props.put("producer.type", conf.get("com.crawler.kafka.producer.type", "async"))
  //props.put("request.required.acks", "1")

  val producterConfig = new ProducerConfig(props)

  val producer = new Producer[String, String](producterConfig)

  override def sendMsg(msg: String): Boolean = {
    val t = System.currentTimeMillis()
    val data = new KeyedMessage[String, String](productTopic, msg)
    producer.send(data)
    logInfo(("sent cost second: " + (System.currentTimeMillis() - t) / 1000))
    //producer.close();
    true
  }

  override def consumeMsg(): String = {
    val msg = Collections.synchronizedList(new ListBuffer[String]())
    KafkaMessageQueue.kafkaBlockQueue.take()
  }

  def consumeKafka() = {
    //consumer
    //val consumerTopic = conf.get("com.crawler.kafka.consumer.topic", "crawler_v1")
    val consumerTopic = Constant(conf).CONSUME_TOPIC
    val config = createConsumerConfig(Constant(conf).KAFKA_ZK, Constant(conf).KAFKA_GROUPID)
    val consumer = Consumer.create(config)

    val topicCountMap = Map(consumerTopic -> 1)
    val consumerMap = consumer.createMessageStreams(topicCountMap);
    val streams = consumerMap.get(consumerTopic).get;

    for (stream <- streams) {

      val it = stream.iterator()

      while (it.hasNext() && it != null) {
        val c = it.next()
        val msg = new String(c.message())
        KafkaMessageQueue.kafkaBlockQueue.put(msg)
        // msg.append(new String(it.next().message()))
      }
      //consumer.shutdown()
    }
  }

  def createConsumerConfig(zookeeper: String, groupId: String): ConsumerConfig = {
    val props = new Properties()
    props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    props.put("auto.offset.reset", "largest")
    props.put("zookeeper.session.timeout.ms", "15000")
    //props.put("zk.connectiontimeout.ms", "15000")
    props.put("zookeeper.sync.time.ms", "500000")
    props.put("auto.commit.interval.ms", "1000")
    val config = new ConsumerConfig(props)
    config
  }

  private var thread = new Thread("kafka consumer ") {
    //setDaemon(true)
    override def run() { consumeKafka() }
  }

  override def size(): Int = {
    KafkaMessageQueue.kafkaBlockQueue.size()
  }

  override def sendMsgLocal(message: String): Boolean = {
    try {
      KafkaMessageQueue.kafkaBlockQueue.put(message)
      true
    } catch {
      case e: Exception =>
        logError("put %s failed in kafka!".format(message), e)
        false
    }
  }

  override def start() = {
    thread.start()
  }
}

object KafkaMessageQueue {
  var kafka: KafkaMessageQueue = null
  val kafkaBlockQueue = new LinkedBlockingQueue[String]

  def apply(conf: CrawlerConf): KafkaMessageQueue = {
    if (kafka == null)
      kafka = new KafkaMessageQueue(conf)
    kafka
  }

  def main(args: Array[String]): Unit = {
    // KafkaMessageQueue.create(new CrawlerConf).sendMsg("dsfewrjkdsjfskdjfk")
    //KafkaMessageQueue.create(new CrawlerConf).sendMsg("dsf345435345435345")
    //new KafkaMessageQueue(new CrawlerConf).sendMsg("dsfewrjkdsjfskdjfk111111111111111111111111")
    println("哈哈    " + KafkaMessageQueue(new CrawlerConf).consumeMsg)
    //Thread.sleep(1000)

  }
}