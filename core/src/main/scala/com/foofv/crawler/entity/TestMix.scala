package com.foofv.crawler.entity

abstract class TestMix {
  
  def doSms = {
    println("TestMix")
  }
}

class TestX extends TestMix{
  override def doSms = {
    println("before TestX")
    super.doSms
    println("before net")
    
  }
}

object TestMix{
  def main(args: Array[String]): Unit = {
    new TestX().doSms
  }
}