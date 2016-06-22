package com.foofv.crawler.parse.master


/**
 * @author soledede
 */
private[crawler]
object RecoveryState extends Enumeration {
 type MasterState = Value

 val STANDBY, ALIVE, RECOVERING, COMPLETING_RECOVERY = Value
}