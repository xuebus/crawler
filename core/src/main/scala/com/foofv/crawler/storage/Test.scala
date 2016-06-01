package com.foofv.crawler.storage

import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.WriteConcern
import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.dao.SalatDAO
import com.novus.salat.annotations.raw.Key
import org.bson.types.ObjectId
import com.sun.beans.decoder.FalseElementHandler
import com.sun.beans.decoder.TrueElementHandler
import com.mongodb.BasicDBList
import com.mongodb.casbah.commons.MongoDBList
import scala.collection.immutable.HashMap
/**
  * @author msfenn
  */
case class Test(var name: String, var age: Int, var cond: TestCondition) extends MongoDBObject {
}

case class TestCondition(var cond: String, var level: Int) {

}

case class ChildInfo(lastUpdated: String = "DateTime.now")
case class Child(_id: ObjectId = new ObjectId,
                 parentId: ObjectId,
                 x: String,
                 childInfo: ChildInfo = ChildInfo(),
                 y: Option[String] = None)
case class Parent(_id: ObjectId = new ObjectId, name: String)

object ParentDAO extends SalatDAO[Parent, ObjectId](collection = MongoConnection()("test")("parent_coll")) {

  val children = new ChildCollection[Child, ObjectId](collection = MongoConnection()("test")("child_coll"),
    parentIdField = "parentId") {}

}

object ListDAO extends SalatDAO[List[Any], ObjectId](collection = MongoConnection()("test")("parent_coll")) {

}

case class MyDAO(val name: String, val list: List[Any]) {

}

object Main {

  def mongodbRefTest() {
    val parent1 = Parent(name = "parent1")
    val parent2 = Parent(name = "parent2")
    val parent3 = Parent(name = "parent3")
    var list = List(parent1, parent2, parent3)
    var dblist = for (item <- list) yield grater[Parent].asDBObject(item)
    //    println(dblist)

    //    ParentDAO.insert(list)
    ListDAO.insert(dblist)
    //    var dbobject = grater[MyDAO].asDBObject(new MyDAO("myList", list))
    val coll = MongoConnection()("test")("parent_coll")
    //    coll.insert(dblist: _*)
    //    coll.insert(dbobject)
    //    dbobject = coll.findOne(MongoDBObject("name" -> "myList")).get
    //    val mydao = grater[MyDAO].asObject(dbobject)
    //    println(mydao.name)
    //    println(mydao.list.getClass)
    //    val lst = dbobject.as[BasicDBList]("list")
    //    println(lst.getClass)
    //    mydao.list.foreach { x => println(x) }
    //    lst.foreach { x => println(x) }
    //    var cl = lst.toList.collect { case s: Parent => s }
    //    println(cl.getClass())
    //    println(cl.size)
    //    ParentDAO.insert(parent1, parent2, parent3)(WriteConcern.Safe)
    //    val child1Parent1 = Child(parentId = parent1._id, x = "child1Parent1", y = Some("child1Parent1"))
    //    val child2Parent1 = Child(parentId = parent1._id, x = "child2Parent1")
    //    val child3Parent1 = Child(parentId = parent1._id, x = "child3Parent1")
    //    val child1Parent2 = Child(parentId = parent2._id, x = "child1Parent2", y = Some("child1Parent2"))
    //    val child2Parent2 = Child(parentId = parent2._id, x = "child2Parent2", y = Some("child2Parent2"))
    //        ParentDAO.children.insert(child1Parent1)
    //    ParentDAO.children.insert(child2Parent1)
    //    ParentDAO.children.insert(child3Parent1)
    //    ParentDAO.children.insert(child1Parent2)
    //    ParentDAO.children.insert(child2Parent2)
    //    val parent1_id = MongoConnection()("test")("parent_coll").findOne(MongoDBObject("name" -> "parent1")).get.get("_id").asInstanceOf[ObjectId]
    //    val parent2_id = MongoConnection()("test")("parent_coll").findOne(MongoDBObject("name" -> "parent2")).get.get("_id").asInstanceOf[ObjectId]
    //    val parent3_id = MongoConnection()("test")("parent_coll").findOne(MongoDBObject("name" -> "parent3")).get.get("_id").asInstanceOf[ObjectId]
    //    //        val r1 = ParentDAO.children.findByParentId(parent1_id).toList
    //    //    val r2 = ParentDAO.children.findByParentId(parent2_id).toList
    //    //    val r3 = ParentDAO.children.findByParentId(parent3_id).toList
    //    //    r1.foreach { x => println(x.x) }
    //    //    println()
    //    //    r2.foreach { x => println(x.x) }
    //    //    println()
    //    //    r3.foreach { x => println(x.x) }
    //    //    val childIds = ParentDAO.children.idsForParentId(parent1_id).toList
    //    //    childIds.foreach { x => println(x) }
    //
    //    val updateQuery = MongoDBObject("$set" -> MongoDBObject("parentId" -> parent3_id))
    //    //    ParentDAO.children.updateByParentId(parent1_id, updateQuery, false, true)
    //    //    ParentDAO.children.updateByParentId(parent2_id, updateQuery, false, true)
    //    //    ParentDAO.children.updateByParentId(parent3_id, updateQuery, false, true)
    //    ParentDAO.children.update(MongoDBObject("x" -> "child2Parent2"), updateQuery, false, false)
  }

  def main(args: Array[String]): Unit = {

    //    mongodbRefTest()
    //    var client: MongoClient = MongoClient("localhost", 27017)
    //    var mongodb: MongoDB = client("test")
    //    var coll = mongodb("User")
    //    var test: Test = new Test("bill", 50, new TestCondition("good", 1))
    //    var dbobject = grater[Test].asDBObject(test)
    //    //    coll.insert(dbobject)
    //    //    var dbobject: MongoDBObject = null
    //    dbobject = coll.findOne(MongoDBObject("name" -> "bill")).get /*.asDBObject*/
    //    println(dbobject.expand[String]("cond.cond").get)
    //    /*println(dbobject.as[String]("name"))
    //    println(dbobject.as[Int]("age"))
    //    println(dbobject.as[TestCondition]("cond").cond)
    //    println(dbobject.as[TestCondition]("cond").level)*/
    //    //    for (item <- coll.find()) if (item.get("name").equals("bill")) {
    //    //      dbobject = item.asDBObject
    //    //    }
    //    /*var*/ test = grater[Test].asObject(dbobject)
    //    print(test.name + ": " + test.age + ": " + test.cond)
    //    //    if (coll.exists { x => if (x.get("name").equals("bill")) true else false }) {
    //    //      println("existing")
    //    //    } else {
    //    //      println("insert")
    //    //      coll.insert(MongoDBObject("name" -> "bill"), WriteConcern.Safe)
    //    //    }
    //    //    println(coll.size)
  }
}