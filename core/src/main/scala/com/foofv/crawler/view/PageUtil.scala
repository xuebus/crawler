package com.foofv.crawler.view

import scala.xml.Node

/**
 * Created by soledede on 2015/9/21.
 */
trait PageUtil {

  def assemblePage(insertPage: => Seq[Node], title: String): Seq[Node] = {
    <html>
      <head>
        {commonHeaders}<title>
        {title}
      </title>
      </head>
      <body>
        <div>
          {insertPage}
        </div>
      </body>
    </html>
  }


  def commonHeaders: Seq[Node] = {
      <meta http-equiv="Content-type" content="text/html; charset=utf-8"/>
        <link rel="stylesheet" href={prependBaseUri("/static/control_webview.css")} type="text/css"/>
      <script src={prependBaseUri("/static/d3.min.js")}></script>
      <script src={prependBaseUri("/static/dagre-d3.min.js")}></script>
      <script src={prependBaseUri("/static/control_webview.js")}></script>
        <script src="http://d3js.org/d3.v3.min.js" charset="utf-8"></script>
  }

  def prependBaseUri(basePath: String = "", resource: String = ""): String = {
    root + basePath + resource
  }

  def root: String = {
    if (System.getenv("APPLICATION_WEB_CONTROL_BASE") != null) {
      System.getenv("APPLICATION_WEB_CONTROL_BASE")
    } else if (System.getProperty("crawler.view.baseUrl") != null) {
      System.getProperty("crawler.view.baseUrl")
    }
    else {
      ""
    }
  }


}
