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
 **/
package com.foofv.crawler.serializer

import java.io._
import java.nio.ByteBuffer
import scala.reflect.ClassTag
import org.apache.zookeeper.server.ByteBufferInputStream
import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.Util

/**
 * @author soledede
 */
private[crawler] class JavaSerializationStream(out: OutputStream, counterReset: Int)
  extends SerializationStream {
  private val objOut = new ObjectOutputStream(out)
  private var counter = 0

  /**
   * Calling reset to avoid memory leak:
   */
  def writeObject[T: ClassTag](t: T): SerializationStream = {
    objOut.writeObject(t)
    counter += 1
    if (counterReset > 0 && counter >= counterReset) {
      objOut.reset()
      counter = 0
    }
    this
  }

  def flush() {
    objOut.flush()
  }

  def close() {
    objOut.close()
  }
}

private[crawler] class JavaDeserializationStream(in: InputStream, loader: ClassLoader)
  extends DeserializationStream {
  private val objIn = new ObjectInputStream(in) {
    override def resolveClass(desc: ObjectStreamClass) =
      Class.forName(desc.getName, false, loader)
  }

  def readObject[T: ClassTag](): T = objIn.readObject().asInstanceOf[T]

  def close() {
    objIn.close()
  }
}


private[crawler] class JavaSerializerInstance(counterReset: Int, defaultClassLoader: ClassLoader)
  extends SerializerInstance {

  override def serialize[T: ClassTag](t: T): ByteBuffer = {
    val bos = new ByteArrayOutputStream()
    val out = serializeStream(bos)
    out.writeObject(t)
    out.close()
    ByteBuffer.wrap(bos.toByteArray)
  }

  override def serializeArray[T: ClassTag](t: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val out = serializeStream(bos)
    out.writeObject(t)
    out.close()
    bos.toByteArray
  }

  override def deserialize[T: ClassTag](bytes: ByteBuffer): T = {
    val bis = new ByteBufferInputStream(bytes)
    val in = deserializeStream(bis)
    in.readObject()
  }

  override def deserialize[T: ClassTag](input: InputStream): T = {
    val in = deserializeStream(input)
    in.readObject()
  }

  override def deserialize[T: ClassTag](bytes: ByteBuffer, loader: ClassLoader): T = {
    val bis = new ByteBufferInputStream(bytes)
    val in = deserializeStream(bis, loader)
    in.readObject()
  }

  override def deserialize[T: ClassTag](bytes: Array[Byte]): T = {
    val bis = new ByteBufferInputStream(ByteBuffer.wrap(bytes))
    val in = deserializeStream(bis)
    in.readObject()
  }

  override def serializeStream(s: OutputStream): SerializationStream = {
    new JavaSerializationStream(s, counterReset)
  }

  override def deserializeStream(s: InputStream): DeserializationStream = {
    new JavaDeserializationStream(s, Util.getContextClassLoader)
  }

  def deserializeStream(s: InputStream, loader: ClassLoader): DeserializationStream = {
    new JavaDeserializationStream(s, loader)
  }
}

class JavaSerializer private(conf: CrawlerConf) extends Serializer with Externalizable {
  private var counterReset = conf.getInt("crawler.serializer.objectStreamReset", 100)

  override def newInstance(): SerializerInstance = {
    val classLoader = defaultClassLoader.getOrElse(Thread.currentThread.getContextClassLoader)
    new JavaSerializerInstance(counterReset, classLoader)
  }

  override def writeExternal(out: ObjectOutput): Unit = Util.tryOrIOException {
    out.writeInt(counterReset)
  }

  override def readExternal(in: ObjectInput): Unit = Util.tryOrIOException {
    counterReset = in.readInt()
  }
}

object JavaSerializer {
  private var instance: JavaSerializer = null

  def apply(conf: CrawlerConf) = {
    if (instance == null) {
      instance = new JavaSerializer(conf)
    }
    instance
  }
}
