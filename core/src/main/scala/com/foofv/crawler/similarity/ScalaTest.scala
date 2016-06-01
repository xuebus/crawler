package com.foofv.crawler.similarity

import com.foofv.crawler.downloader.Downloader

/**
 * Created by msfenn on 16/09/15.
 */
class ScalaTest {

  def callback(downloader: Downloader, downloader1: Downloader): Unit = {

    println("i'm callback")
  }

  def funcTest(string: String, callback: (Downloader, Downloader) => Unit): Unit = {

    callback(null, null)
  }
}

object Main1 extends App {

  val test = new ScalaTest
  test.funcTest("", test.callback)
}
