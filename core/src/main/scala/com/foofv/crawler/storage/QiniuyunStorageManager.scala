package com.foofv.crawler.storage

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.serializer.Serializer
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth
import com.qiniu.storage.BucketManager
import scala.reflect.ClassTag
import java.nio.ByteBuffer
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods.CloseableHttpResponse

class QiniuyunStorageManager private (conf:CrawlerConf) extends StorageManager {
  
  private val ACCESS_KEY: String = conf.get("crawler.storagemanager.qiniuyun,accesskey", "N0R-mSBcA4jHTZ_ZHiW8WKCUfsmVxte8ohx95jLp")
  private val SECRET_KEY: String = conf.get("crawler.storagemanager.qiniuyun,secretkey","xADmek26W8AcnWTqpBJ7hDWEFtYQRiIukTBUTVS9")
  private val BUCKET: String = conf.get("crawler.storagemanager.qiniuyun.bucket","xms57")
  private val URL_PREFIX: String = conf.get("crawler.storagemanager.qiniuyun.url","http://7xk0rk.com1.z0.glb.clouddn.com/")
  private val auth: Auth = Auth.create(ACCESS_KEY, SECRET_KEY);
  private val bucketManager: BucketManager = new BucketManager(auth);
  private val uploadToken: String = auth.uploadToken(BUCKET);
  private val uploadManager: UploadManager = new UploadManager();
  private val serializer = Serializer("java",conf).newInstance()
  
  override def getByKey[T: ClassTag](key: String): T = {
    var builder: HttpClientBuilder = HttpClientBuilder.create();
    var httpClient: CloseableHttpClient = builder.build()
    var response: CloseableHttpResponse = null
    var value: T = null.asInstanceOf[T]
    try {
      val httpGet = new HttpGet(auth.privateDownloadUrl(URL_PREFIX + key))
      response = httpClient.execute(httpGet)
      val statusCode = response.getStatusLine.getStatusCode
      if (statusCode == 200) {
        val entity = response.getEntity()
        val bytes = EntityUtils.toByteArray(entity);
        try {
          value = serializer.deserialize(ByteBuffer.wrap(bytes))        
        } catch {
          case t: Throwable => logError(s"[$key] deserialize ERROR", t)
        }
      } else {
        val statusLine = response.getStatusLine.getReasonPhrase
        logError(s"get key[$key] ERROR: $statusLine")
      }
      
    } catch {
      case t: Throwable => logError(s"put [$key] to Qiniuyun ERROR", t)
    } finally {
      HttpClientUtils.closeQuietly(response)
      HttpClientUtils.closeQuietly(httpClient)
    }
    value
  }
  
  override def listByKey[T: ClassTag](key: String): Seq[T] = {
    null.asInstanceOf[Seq[T]]
  }
  
  override def put[T: ClassTag](entity: T): Boolean = {
    throw new UnsupportedOperationException("unsupported")
  }
  
  override def putByKey[T: ClassTag](key: String,entity: T): Boolean = {
    val isOK = false
    try {
      val bytes = serializer.serialize(entity).array()
      val resp = uploadManager.put(bytes, key, uploadToken)
      val isOK = resp.isOK()
      val respStr = resp.bodyString()
      logDebug(s"put $key to Qiniuyun, result:$respStr ")
    } catch {
      case t: Throwable => logError(s"put [$key] to Qiniuyun ERROR", t)
    }
    isOK
  }

}

object QiniuyunStorageManager {
  
  def main(args: Array[String]): Unit = {
    testGetKey2
  }
  
  def test = {
    val conf:CrawlerConf = new CrawlerConf
    val qiniu = new QiniuyunStorageManager(conf)
    qiniu.putByKey("test21", "qiniu")
  }
  
  def testGetKey = {
    val conf:CrawlerConf = new CrawlerConf
    val qiniu = new QiniuyunStorageManager(conf)
    
    qiniu.putByKey("test-xms", "xms")
    val result = qiniu.getByKey[String]("test-xms")
    println(result)
  }
  
  def testGetKey2 = {
    val conf:CrawlerConf = new CrawlerConf
    val qiniu = new QiniuyunStorageManager(conf)
    
    qiniu.putByKey("test-xms122", new Student("xms",20))
    val result = qiniu.getByKey[Student]("test-xms122")
    println(result)
    println(result.name)
    println(result.age)
  }
  
  class Student(val name: String,val age: Int) extends Serializable
}