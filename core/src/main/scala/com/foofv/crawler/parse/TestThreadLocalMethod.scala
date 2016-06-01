package com.foofv.crawler.parse

class TestThreadLocalMethod private {
  
    def testa() = {
    var f =0
    f += 1
    println(Thread.currentThread().getName+f)
  }
}



object TestThreadLocalMethod {
var e: TestThreadLocalMethod= null

def apply(): TestThreadLocalMethod = {
if(e==null) e = new TestThreadLocalMethod()
e
}
  
  def main(args: Array[String]): Unit = {
    
    for(a <- 0 to 10){
    new Thread(new Runnable {
    def run() {
      TestThreadLocalMethod().testa()
    }
  }).start()
  }
  }
}