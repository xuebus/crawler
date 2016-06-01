package com.foofv.crawler.util

/**
 * Created by soledede on 2015/9/16.
 */
object LIstTest {

  def main(args: Array[String]) {
    print(List("dffd","sdfse","和").foldLeft("")((sum,n)=>sum+n))
  }

}
