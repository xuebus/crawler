package com.foofv.crawler.leaderelection

object LeadershipStatus extends Enumeration{
  type LeadershipStatus = Value
  val LEADER, NOT_LEADER = Value
}