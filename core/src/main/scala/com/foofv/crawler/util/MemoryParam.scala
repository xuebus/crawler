package com.foofv.crawler.util

object MemoryParam {
  
  def unapply(str: String): Option[Int] = {
    try {
      Some(Util.memoryStringToMb(str))
    } catch {
      case e: NumberFormatException => None
    }
  }

}