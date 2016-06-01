package com.foofv.crawler.antispamming

import java.io.{InputStreamReader, BufferedReader}

/**
 * Created by soledede on 2015/8/28.
 */
object ASDLTest {

def main (args: Array[String] ) {
connAdsl("","XMS_Mobile","Mobile5710")
Thread.sleep(1000);
cutAdsl("");
Thread.sleep(1000);
//再连，分配一个新的IP
connAdsl("","XMS_Mobile","Mobile5710")
}

/**
 * 执行CMD命令
 */
def executeCmd( strCmd: String) : String = {
val p: Process = Runtime.getRuntime().exec("cmd /c " + strCmd)
 val sbCmd: java.lang.StringBuilder = new java.lang.StringBuilder()
 val br: BufferedReader = new BufferedReader(new InputStreamReader(p
.getInputStream()))
var line: String = ""
while ((line = br.readLine()) != null) {
sbCmd.append(line + "\n")
}
sbCmd.toString()
}

/**
 * 连接ADSL
 */
def connAdsl(dslTitle: String, adslName: String,adslPass: String) : Boolean  = {
println("正在建立连接...")
val adslCmd: String = "rasdial " + dslTitle + " " + adslName + " " + adslPass
val tempCmd: String = executeCmd(adslCmd);
// 判断是否连接成功
if (tempCmd.indexOf("已连接") > 0) {
println("已成功建立连接.")
return true
} else {
System.err.println(tempCmd)
System.err.println("建立连接失败")
return false
}
}

/**
 * 断开ADSL
 */
def  cutAdsl(adslTitle: String) : Boolean = {
val  cutAdsl: String = "rasdial " + adslTitle + " /disconnect"
val result: String = executeCmd(cutAdsl)

if (result.indexOf("没有连接") != -1){
System.err.println(adslTitle + "连接不存在!")
return false
} else {
System.out.println("连接已断开")
return true;
}
}


}
