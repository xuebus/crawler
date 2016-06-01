package com.foofv.crawler.util.listener

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Semaphore, LinkedBlockingQueue, CopyOnWriteArrayList}

import com.foofv.crawler.util.Logging

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 * Created by soledede on 2015/9/17.
 */
trait ListenerWaiter[L <: AnyRef, E] extends Logging {

  self =>

  private[crawler] val listeners = new CopyOnWriteArrayList[L]


  private val EVENT_QUEUE_CAPACITY = 10000
  private val eventQueue = new LinkedBlockingQueue[E](EVENT_QUEUE_CAPACITY)

  private val started = new AtomicBoolean(false)
  private val stopped = new AtomicBoolean(false)


  private var processingEvent = false

  private val eventLock = new Semaphore(0)

  val name = "asynListenerThread"

  private val listenerThread = new Thread(name) {
    setDaemon(true)

    override def run(): Unit = {
      while (true) {
        eventLock.acquire()
        self.synchronized {
          processingEvent = true
        }
        try {
          val event = eventQueue.poll
          if (event == null) {
            if (!stopped.get) {
              throw new IllegalStateException("Polling `null` from eventQueue means" +
                " the listener waiter has been stopped. So `stopped` must be true")
            }
            return
          }
          postToAll(event)
        } finally {
          self.synchronized {
            processingEvent = false
          }
        }
      }
    }
  }


  def start() {
    if (started.compareAndSet(false, true)) {
      listenerThread.start()
    } else {
      throw new IllegalStateException(s"$name already started!")
    }
  }

  def post(event: E) {
    if (stopped.get) {
      logError(s"$name has already stopped! Dropping event $event")
      return
    }
    val eventAdded = eventQueue.offer(event)
    if (eventAdded) {
      eventLock.release()
    } else {
      onDropEvent(event)
    }
  }





  private def queueIsEmpty: Boolean = synchronized {
    eventQueue.isEmpty && !processingEvent
  }


  def stop() {
    if (!started.get()) {
      throw new IllegalStateException(s"Attempted to stop $name that has not yet started!")
    }
    if (stopped.compareAndSet(false, true)) {

      eventLock.release()
      listenerThread.join()
    } else {
      // Keep quiet
    }
  }


  def onDropEvent(event: E): Unit

  final def addListener(listener: L) {
    listeners.add(listener)
  }


  final def postToAll(event: E): Unit = {

    val iter = listeners.iterator
    while (iter.hasNext) {
      val listener = iter.next()
      try {
        onPostEvent(listener, event)
      } catch {
        case NonFatal(e) =>
          logError("Listener postToAll failed", e)
      }
    }
  }


  def onPostEvent(listener: L, event: E): Unit



}
