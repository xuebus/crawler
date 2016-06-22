package com.foofv.crawler.parse.master

/**
 * @author soledede
 */
private[crawler] object ParserMasterMsg {

  // LeaderElection to Master

  case object ElectedLeader

  case object RevokedLeadership

  case object CheckTimeOutForWorker

  case object CheckTimeOutForMaster

}