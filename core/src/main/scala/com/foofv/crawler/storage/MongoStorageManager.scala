package com.foofv.crawler.storage

import java.util
import com.foofv.crawler.parse.topic.entity.TakeoutMerchant
import org.bson.Document
import org.bson.types.ObjectId
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.{ClassTag, _}
import com.foofv.crawler.CrawlerConf
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import com.foofv.crawler.entity.ResObj

/**
 * Created by msfenn on 16/07/15.
 */
class MongoStorageManager private(conf: CrawlerConf) extends StorageManager {

  override def put[T: ClassTag](entity: T): Boolean = {


    if (entity.isInstanceOf[AnyRef] && !entity.isInstanceOf[(Array[AnyRef], Boolean)] && !entity.isInstanceOf[(Class[AnyRef], Array[AnyRef], Array[String], Array[Array[AnyRef]], Boolean)] && !entity.isInstanceOf[(String, Seq[mutable.Map[String, AnyRef]])]) {
      MongoStorage.saveEntity(entity)
    } else if (entity.isInstanceOf[(_, _)]) {
      try {
        val tuEntity = entity.asInstanceOf[(String, Seq[Map[String, AnyRef]])]
        val oE = tuEntity._2
        if (oE == null) {
          logInfo("no schema object,We have returned......")
          return false
        } else {
          try {
            val docsToSave: util.List[Document] = new util.ArrayList[Document]
            import scala.collection.JavaConversions._
            for (doc <- oE) {
              val javaDoc = doc.map { m =>
                if (m._2.isInstanceOf[Seq[mutable.Map[String, AnyRef]]]) {
                  val listBuffer = m._2.asInstanceOf[Seq[mutable.Map[String, AnyRef]]]
                  val conList = listBuffer.map(_.asJava).asJava
                  (m._1, conList)
                } else (m._1, m._2)
              }
              docsToSave.add(new Document(javaDoc))
            }
            MongoStorage.saveDocuments(tuEntity._1, docsToSave)
            true
          }
          catch {
            case e: Exception if !e.isInstanceOf[java.lang.ClassCastException] => {
              logError("save entity to mongo failed", e);
              false
            }
          }
        }
      }
      catch {
        case e: java.lang.ClassCastException => {
          try {
            val tuple = entity.asInstanceOf[(String, mutable.Map[String, Array[Byte]])]
            MongoStorage.saveDocument(tuple._1, new Document(tuple._2))
            true
          }
          catch {
            case e: java.lang.ClassCastException =>
              try {
                val tuple = entity.asInstanceOf[(Array[AnyRef], Boolean)]
                MongoStorage.saveEntities(tuple._1, tuple._2)
              }
              catch {
                case e: Exception => logError("save entity to mongo failed (Array[AnyRef], Boolean)", e); false
              }
          }
        }
      }
    } else if (entity.isInstanceOf[(Class[AnyRef], Array[AnyRef], Array[String], Array[Array[AnyRef]], Boolean)]) {
      val quintet = entity.asInstanceOf[(Class[AnyRef], Array[AnyRef], Array[String], Array[Array[AnyRef]], Boolean)]
      if (quintet._3.forall(item => item.isInstanceOf[String]) && quintet._4.forall(item => item.isInstanceOf[Array[AnyRef]])) {
        val clazz = quintet._1
        MongoStorage.updateFieldsByKey(clazz, quintet._2, quintet._3, quintet._4, quintet._5)
      } else
        false
    } else
      false

    /*
    if (entity.isInstanceOf[AnyRef] && !entity.isInstanceOf[(Array[AnyRef], Boolean)] && !entity.isInstanceOf[(Class[AnyRef], Array[AnyRef], Array[String], Array[Array[AnyRef]], Boolean)] && !entity.isInstanceOf[(String, Seq[mutable.Map[String, AnyRef]])]) {
      MongoStorage.saveEntity(entity)
    } else if (entity.isInstanceOf[(String, Seq[Map[String, AnyRef]])] && !entity.isInstanceOf[(Array[AnyRef], Boolean)]) {
      val tuEntity = entity.asInstanceOf[(String, Seq[Map[String, AnyRef]])]
      if (tuEntity._2 == null) {
        logInfo("no schema object,We have returned......")
        return false
      }
      try {
        MongoStorage.saveDocuments(tuEntity._1, tuEntity._2.map(_.asJava).asJava)
        true
      }
      catch {
        case e: Exception => {logError("save entity to mongo failed", e); false}
      }
    } else if (entity.isInstanceOf[(Array[AnyRef], Boolean)] && !entity.isInstanceOf[(Class[AnyRef], Array[AnyRef], Array[String], Array[Array[AnyRef]], Boolean)]) {
      val tuple = entity.asInstanceOf[(Array[AnyRef], Boolean)]
      MongoStorage.saveEntities(tuple._1, tuple._2)
    } else if (entity.isInstanceOf[(Class[AnyRef], Array[AnyRef], Array[String], Array[Array[AnyRef]], Boolean)]) {
      val quintet = entity.asInstanceOf[(Class[AnyRef], Array[AnyRef], Array[String], Array[Array[AnyRef]], Boolean)]
      if (quintet._3.forall(item => item.isInstanceOf[String]) && quintet._4.forall(item => item.isInstanceOf[Array[AnyRef]])) {
        val clazz = quintet._1
        MongoStorage.updateFieldsByKey(clazz, quintet._2, quintet._3, quintet._4, quintet._5)
      } else
        false
    } else
      false*/
  }

  override def getByKey[T: ClassTag](key: String): T = {
    MongoStorage.getByKey(classTag[T].runtimeClass.asInstanceOf[Class[T]], key)
  }

  override def listByKey[T: ClassTag](key: String): Seq[T] = {
    MongoStorage.getList(classTag[T].runtimeClass.asInstanceOf[Class[T]], key, key).asInstanceOf[Seq[T]]
  }

  override def putByKey[T: ClassTag](key: String, entity: T): Boolean = {
    MongoStorage.updateEntityByKey(classTag[T].runtimeClass.asInstanceOf[Class[T]], "_id", key, entity)
  }
}

object MongoStorageManager extends App {

  var singleton: MongoStorageManager = null
  val lock = new Object()

  def apply(conf: CrawlerConf): MongoStorageManager = {
    if (singleton == null) {
      lock.synchronized{
        if (singleton == null) {
          singleton = new MongoStorageManager(conf)
        }
      }
    }
    singleton
  }


  def testCa() = {
    var s: Any = null
    //s = (Array(1,2,3),false)
    s match {
      case t: (Array[AnyRef], Boolean) => {}
      case t2: (String, Seq[Map[String, AnyRef]], AnyRef) => print("")
      case _ =>
    }
  }

  //  val user = new User()
  //  val id = new ObjectId("55b72cde6fe44368c2ec9d0c")
  //  user.id = id
  //  user.name = "jobs"
  //  user.age = 251
  //
  //  val company1 = new Company()
  //  company1.name = "microsoft"
  //  company1.stuffs = 11
  //  val company2 = new Company()
  //  company2.name = "google"
  //  company2.stuffs = 10
  //
  //  val company3 = new Company()
  //  company3.name = "Baidu"
  //  company3.stuffs = 9
  //  val company4 = new Company()
  //  company4.name = "Ali"
  //  company4.stuffs = 8
  //
  //  val address1 = new Address()
  //  address1.country = "USA"
  //  address1.company = new util.ArrayList[Company]()
  //  address1.company.add(company1)
  //  address1.company.add(company2)
  //
  //  val address2 = new Address()
  //  address2.country = "CHINA"
  //  address2.company = new util.ArrayList[Company]()
  //  address2.company.add(company3)
  //  address2.company.add(company4)
  //
  //  user.address = new util.ArrayList()
  //  user.address.add(address1)
  //  user.address.add(address2)

  //  val ids = Array(new ObjectId("55b72cde6fe44368c2ec9d0b"), new ObjectId("55b72cde6fe44368c2ec9d0c"))
  //  val properties = Array("name", "age", "address")
  //  val values = Array.ofDim[AnyRef](ids.length, properties.length)
  //  values(0)(0) = "BILL"
  //  values(0)(1) = 10.asInstanceOf[AnyRef]
  //  val addrList1 = new util.ArrayList[Address]()
  //  addrList1.add(address1)
  //  values(0)(2) = addrList1
  //
  //  values(1)(0) = "JOBS"
  //  values(1)(1) = 20.asInstanceOf[AnyRef]
  //  val addrList2 = new util.ArrayList[Address]()
  //  addrList2.add(address2)
  //  values(1)(2) = addrList2
  //
  def test() = {
    val db = MongoStorageManager(new CrawlerConf())
    //
    //  //  db.put((Array(user), false))
    //  db.put((classOf[User], ids, properties, values, false))

    val takeout = db.getByKey[TakeoutMerchant]("waimai.baidu.com/B8A78AAC0E908BF7649804BC82EE4532/1")
    println(takeout.getExtraServices)
    println()
    println(takeout.getName)
    takeout.getMenus.asScala.foreach {
      menu => {
        println(menu.getMenuName)
        menu.getFoods.asScala.foreach {
          food => {
            println("\t" + food.getFoodName)
          }
        }
      }
    }
  }

}
