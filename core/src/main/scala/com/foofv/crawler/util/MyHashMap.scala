package com.foofv.crawler.util

import scala.collection.mutable.HashMap
import redis.ByteStringFormatter
import akka.util.ByteString
import redis.ByteStringSerializer
import redis.ByteStringDeserializer
import scala.concurrent.ExecutionContext.Implicits._
import com.foofv.crawler.redis.cli.RedisOps
import com.foofv.crawler.CrawlerConf

class MyHashMap[A, B] extends HashMap[A, B]

object MyHashMap {
  implicit val byteStringFormatter = new ByteStringFormatter[MyHashMap[String,String]] {
    def serialize(hashMap: MyHashMap[String,String]): ByteString = {
      val v: StringBuilder = new StringBuilder
      hashMap.map(f => v.append(f._1+"-"+f._2+"|"))
      print(v.toString())
      ByteString(
         v.toString()
        )
    }

    def deserialize(bs: ByteString): MyHashMap[String,String] = {
      val r = bs.utf8String.split('|').toList
      val hashMap = new MyHashMap[String,String]
      val list = r.filter { x => !(x.trim().isEmpty()) }
      list.map { x => {val temp = x.split('-'); hashMap(temp(0))=temp(1)} }
      println(hashMap.toString())
      hashMap
    }
  }

  def tests[d: ByteStringSerializer](r: d) ={
    System.err.println(r.getClass())
  }
  
  def deSerializerByString[T: ByteStringDeserializer](s: ByteString)(implicit f: T):T = {
    System.err.print(s)
    null.asInstanceOf[T]
  }

  def deSerializerByString1(s: ByteString)(implicit f: ByteStringDeserializer[MyHashMap[String,String]]) = {
    
    System.err.print(f.deserialize(s))
  }
}

object Test{
  def main(args: Array[String]): Unit = {
    testSerialize()
    testDeserialize()
  }
  
  def testSerialize(){
    val hashMap = new MyHashMap[String,String]
    hashMap("a")="AAA"
    hashMap("a2")="AAA2"
    println(MyHashMap.tests[MyHashMap[String,String]](hashMap))
    
    val conf = new CrawlerConf
    val redis: RedisOps = RedisOps.createRedis("akka", conf)
    val key = "test-hmset"
    redis.putKvHashMap(key,Map("we"->hashMap))
  }
  
  def testDeserialize(){
    val conf = new CrawlerConf
    val redis: RedisOps = RedisOps.createRedis("akka", conf)
    val key = "test-hmset"
    val hashMap = redis.getKvHashMap(key,Seq("we"))
    System.err.println(hashMap)
    
    System.err.println(ByteString(hashMap.head))
    
    //MyHashMap.deSerializerByString[MyHashMap[String,String]](ByteString(hashMap.head))
    MyHashMap.deSerializerByString1(ByteString(hashMap.head))
  }
}
