package com.foofv.crawler.hbase

/**
 * Copyright [2015] [soledede]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.{ ObjectInputStream, ByteArrayInputStream }
import org.apache.hadoop.hbase.{ HColumnDescriptor, HTableDescriptor, HBaseConfiguration }
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import scala.collection.mutable
import scala.util.parsing.json.JSONArray
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import com.foofv.crawler.serializer.Serializer
import com.foofv.crawler.CrawlerConf
import scala.reflect.ClassTag
import java.io.IOException
import com.foofv.crawler.util.Logging

/**
 * @author soledede
 */
private[crawler]
object HbaseTool  extends Logging{

  val table = new mutable.HashMap[String, HTable]()
  var conf = HBaseConfiguration.create()

  def setConf(c: Configuration) = {
    conf = c
  }

  def getTable(tableName: String): HTable = {

    table.getOrElse(tableName, {
      println("----new connection ----")
      val tbl = new HTable(conf, tableName)
      table(tableName) = tbl
      tbl
    })
  }

  def createTable(tableName: String, columnFamily: String): Unit = {
    val admin = new HBaseAdmin(conf)
    if (!admin.isTableAvailable(tableName)) {
      print("Table Not Exists! Create Table")
      val tableDesc = new HTableDescriptor(tableName)
      tableDesc.addFamily(new HColumnDescriptor(columnFamily.getBytes()))
      admin.createTable(tableDesc)
    }
  }

  def getSingleValue(tableName: String, rowKey: String, family: String, qualifier: String): JSONArray = {
    val table_t = getTable(tableName)
    val row1 = new Get(Bytes.toBytes(rowKey))
    val HBaseRow = table_t.get(row1)
    var obh: AnyRef = null
    //new JSONArray[List[String]]
    //var bv = new Array[Byte](0)
    // var bv = new String
    if (HBaseRow != null && !HBaseRow.isEmpty) {
      val b = new ByteArrayInputStream(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier)))
      val o = new ObjectInputStream(b)
      obh = o.readObject()
      o.close()
      //bv = new String(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifiers)),"UTF-8")
    }
    obh.asInstanceOf[JSONArray]
  }
  
  
 def getSingleValue[T: ClassTag](tableName: String, rowKey: String, family: String, qualifier: String): T = {
    val table_t = getTable(tableName)
    val row1 = new Get(Bytes.toBytes(rowKey))
    val HBaseRow = table_t.get(row1)
    //var obh: AnyRef = null
    //new JSONArray[List[String]]
    //var bv = new Array[Byte](0)
    // var bv = new String
    if (HBaseRow != null && !HBaseRow.isEmpty) {
      return Serializer("java",new CrawlerConf).newInstance().deserialize(ByteBuffer.wrap(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier))))
     /* val b = new ByteArrayInputStream(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier)))
      val o = new ObjectInputStream(b)
      obh = o.readObject()
      o.close()*/
      //bv = new String(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifiers)),"UTF-8")
    }
    null.asInstanceOf[T]
  }
  
 
 def getSingleValueByClass[T](tableName: String, rowKey: String, family: String, qualifier: String): T = {
    val table_t = getTable(tableName)
    val row1 = new Get(Bytes.toBytes(rowKey))
    val HBaseRow = table_t.get(row1)
    var obh: AnyRef = null
    //new JSONArray[List[String]]
    //var bv = new Array[Byte](0)
    // var bv = new String
    if (HBaseRow != null && !HBaseRow.isEmpty) {
      val b = new ByteArrayInputStream(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier)))
      val o = new ObjectInputStream(b)
      obh = o.readObject()
      o.close()
      //bv = new String(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifiers)),"UTF-8")
    }
    obh.asInstanceOf[T]
  }
 
  def getValue(tableName: String, rowKey: String, family: String, qualifiers: Array[String]): Array[(String, String)] = {
    var result: AnyRef = null
    val table_t = getTable(tableName)
    val row1 = new Get(Bytes.toBytes(rowKey))
    val HBaseRow = table_t.get(row1)
    if (HBaseRow != null && !HBaseRow.isEmpty) {
      result = qualifiers.map(c => {
        (tableName + "." + c, Bytes.toString(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(c))))
      })
    } else {
      result = qualifiers.map(c => {
        (tableName + "." + c, "null")
      })
    }
    result.asInstanceOf[Array[(String, String)]]
  }

  def getMapSingleValue(tableName: String, rowKey: String, family: String, qualifiers: String): String = {
    var result: AnyRef = null
    val table_t = getTable(tableName)
    val row1 = new Get(Bytes.toBytes(rowKey))
    val HBaseRow = table_t.get(row1)
    if (HBaseRow != null && !HBaseRow.isEmpty) {
      result = Bytes.toString(HBaseRow.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifiers)))
    }
    result.asInstanceOf[String]
  }

  def putValue(tableName: String, rowKey: String, family: String, qualifierValue: Array[(String, String)]) {
    val table = getTable(tableName)
    val new_row = new Put(Bytes.toBytes(rowKey))
    qualifierValue.map(x => {
      var column = x._1
      val value = x._2
      val tt = column.split("\\.")
      if (tt.length == 2) column = tt(1)
      if (!(value.isEmpty))
        new_row.add(Bytes.toBytes(family), Bytes.toBytes(column), Bytes.toBytes(value))
    })
    table.put(new_row)
  }

  def putSingleValue(tableName: String, rowKey: String, family: String, qualifier: String, cValue: String) {
    val table = getTable(tableName)
    val new_row = new Put(Bytes.toBytes(rowKey))
    var column = qualifier
    val value = cValue
    val tt = column.split("\\.")
    if (tt.length == 2) column = tt(1)
    if (!(value.isEmpty))
      new_row.add(Bytes.toBytes(family), Bytes.toBytes(column), Bytes.toBytes(value))
    table.put(new_row)
  }

  def putSingleValue[T: ClassTag](tableName: String, rowKey: String, family: String, qualifier: String, cValue: T): Boolean = {
    val table = getTable(tableName)
    val new_row = new Put(Bytes.toBytes(rowKey))
    var column = qualifier
    val value = cValue
    val tt = column.split("\\.")
    if (tt.length == 2) column = tt(1)
    if (value == null) return false
    /*val bf = ByteBuffer.allocate(4096)
    val outputStream = new java.io.ByteArrayOutputStream()
    val ob = new ObjectOutputStream(outputStream)
    ob.writeObject(value)
    ob.flush()
    bf.put(outputStream.toByteArray())
    ob.close()*/
    new_row.add(Bytes.toBytes(family), Bytes.toBytes(column), Bytes.toBytes(Serializer("java",new CrawlerConf).newInstance().serialize(value)))
    table.put(new_row)
    table.flushCommits()
    true
  }
  
    def putSingleValueClass[T](tableName: String, rowKey: String, family: String, qualifier: String, cValue: T): Boolean = {
    val table = getTable(tableName)
    val new_row = new Put(Bytes.toBytes(rowKey))
    var column = qualifier
    val value = cValue
    val tt = column.split("\\.")
    if (tt.length == 2) column = tt(1)
    if (value == null) return false
    val bf = ByteBuffer.allocate(4096)
    val outputStream = new java.io.ByteArrayOutputStream()
    val ob = new ObjectOutputStream(outputStream)
    ob.writeObject(value)
    ob.flush()
    bf.put(outputStream.toByteArray())
    ob.close()
    new_row.add(Bytes.toBytes(family), Bytes.toBytes(column), Bytes.toBytes(bf))
    table.put(new_row)
    table.flushCommits()
    true
  }
    
    
  
     def deleteRow(tableName: String,rowkey: String): Boolean = {  
        try {  
          val table = getTable(tableName)
            val del = new Delete(rowkey.getBytes());  
              
            table.delete(del);  
           logDebug("delete success !");  
              return true
        } catch {
          case e: IOException => logError("删除失败！", e)
          return false
        }
    }  

  val family = "F"
}




