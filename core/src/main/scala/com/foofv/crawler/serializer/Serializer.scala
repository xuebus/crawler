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
package com.foofv.crawler.serializer

import java.io._
import java.nio.ByteBuffer
import scala.reflect.ClassTag
import com.foofv.crawler.CrawlerEnv
import com.foofv.crawler.util.NextIterator
import com.foofv.crawler.CrawlerConf

/**
 * @author:soledede
 * @email:wengbenjue@163.com
 *                  the serializer of thead safe
 */
abstract class Serializer {

  /**
   * Default ClassLoader to use in deserialization. Implementations of [[Serializer]] should
   * make sure it is using this when set.
   */
  @volatile protected var defaultClassLoader: Option[ClassLoader] = None

  /**
   * Sets a class loader for the serializer to use in deserialization.
   *
   * @return this Serializer object
   */
  def setDefaultClassLoader(classLoader: ClassLoader): Serializer = {
    defaultClassLoader = Some(classLoader)
    this
  }

  /** Creates a new [[SerializerInstance]]. */
  def newInstance(): SerializerInstance
}

object Serializer {
  def apply(serializeType: String, conf: CrawlerConf): Serializer = {
    serializeType match {
      case "java" => getSerializer(JavaSerializer(conf))
      case _ => null.asInstanceOf[Serializer]
    }
  }

  def getSerializer(serializer: Serializer): Serializer = {
    if (serializer == null) CrawlerEnv.get.serializer else serializer
  }

  def getSerializer(serializer: Option[Serializer]): Serializer = {
    serializer.getOrElse(CrawlerEnv.get.serializer)
  }
}

/**
 * :: DeveloperApi ::
 * An instance of a serializer, for use by one thread at a time.
 */
abstract class SerializerInstance {
  def serialize[T: ClassTag](t: T): ByteBuffer

  def serializeArray[T: ClassTag](t: T): Array[Byte] = {
    null
  }

  def deserialize[T: ClassTag](bytes: ByteBuffer): T

  def deserialize[T: ClassTag](bytes: ByteBuffer, loader: ClassLoader): T

  def deserialize[T: ClassTag](input: InputStream): T = {
    null.asInstanceOf[T]
  }

  def deserialize[T: ClassTag](bytes: Array[Byte]): T = {
    null.asInstanceOf[T]
  }

  def serializeStream(s: OutputStream): SerializationStream

  def deserializeStream(s: InputStream): DeserializationStream
}

abstract class SerializationStream {
  def writeObject[T: ClassTag](t: T): SerializationStream

  def flush(): Unit

  def close(): Unit

  def writeAll[T: ClassTag](iter: Iterator[T]): SerializationStream = {
    while (iter.hasNext) {
      writeObject(iter.next())
    }
    this
  }
}

abstract class DeserializationStream {
  def readObject[T: ClassTag](): T

  def close(): Unit

  def asIterator: Iterator[Any] = new NextIterator[Any] {
    override protected def getNext() = {
      try {
        readObject[Any]()
      } catch {
        case eof: EOFException =>
          finished = true
      }
    }

    override protected def close() {
      DeserializationStream.this.close()
    }
  }
}
