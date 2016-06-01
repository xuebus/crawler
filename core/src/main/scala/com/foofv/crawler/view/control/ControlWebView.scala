package com.foofv.crawler.view.control

import javax.servlet.http.HttpServletRequest

import com.foofv.crawler.CrawlerConf
import com.foofv.crawler.util.constant.Constant
import com.foofv.crawler.util.listener._
import com.foofv.crawler.util.{Json4sHelp, Logging}
import com.foofv.crawler.view.{PageUtil, WebViewPage, WebView}
import org.json4s.JsonAST.{JNothing, JValue}
import com.foofv.crawler.view.JettyUtil._
import scala.collection.mutable
import scala.xml.Node
import scala.collection.JavaConversions._


/**
 * Created by soledede on 2015/9/15.
 */
private[crawler] class ControlWebView(requestedPort: Int, conf: CrawlerConf) extends WebView(requestedPort, conf, name = "ControlWebView") with Logging {
  val WEB_STATIC_RESOURCE_DIR = Constant(conf).WEB_STATIC_RESOURCE_DIR

  /** Initialize all components of the server. */
  override def initialize(): Unit = {
    attachHandler(createStaticHandler(WEB_STATIC_RESOURCE_DIR, "/static"))
    val controlPage = new ControlWebPage()
    attachPage(controlPage)
  }

  initialize()
}

/*   <h1 style="color:red; text-align:center">welcome crawler!</h1>
         <svg width="100%" height="100%">
           <circle cx="500" cy="300" r="100" stroke="#ff0" stroke-width="5" fill="red"/>
           <polygon points="50 160 55 180 70 180 60 190 65 205 50 195 35 205 40 190 30 180 45 180"
                    stroke="green" fill="transparent" stroke-width="5"/>
           <text x="500" y="500" font-size="60" text-anchor="middle" fill="red">SVG</text>
           <svg width="5cm" height="4cm">
             <image xlink:href="http://s1.95171.cn/b/img/logo/95_logo_r.v731222600.png" x="0" y="0" height="100px" width="100px"/>
           </svg>
         </svg>*/

private[crawler] class ControlWebPage extends WebViewPage("") with PageUtil {

  val w = ManagerListenerWaiter()
  val keys = w.post(Keys("task_trace_*"))


  override def render(request: HttpServletRequest): Seq[Node] = {
    val keys = w.post(Keys("task_trace_*"))


    //val allTaskNum = w.post(JobTaskAdded())
    val showPage = {
      <div>
        <img src="http://s1.95171.cn/b/img/logo/95_logo_r.v731222600.png"/>{if (ControlWebPage.jobInfoCache.size == 0) {
        <h4 style="color:red">No data,Please refresh your browser</h4>

      }}{ControlWebPage.jobInfoCache.map { job =>
        if (job._1.equalsIgnoreCase(CrawlerTaskTraceListener.TASK_COMPLETED_NUM)) {
          <h2>Completed Tasks Number</h2>
            <table class="bordered">
              <thead>
                <tr>
                  <th>JobId</th>
                  <th>JobName</th>
                  <th>Number</th>
                </tr>
              </thead>{job._2.map { s =>
              <tr>
                <td>
                  {s._1._1}
                </td>
                <td>
                  {s._1._2}
                </td>
                <td>
                  {s._2}
                </td>
              </tr>
            }}
            </table>
              <br/>
              <br/>
        }
        else if (job._1.equalsIgnoreCase(CrawlerTaskTraceListener.TASK_FAIELD_NUM)) {
          <h2>Faield Tasks Number</h2>
            <table class="bordered">
              <thead>
                <tr>
                  <th>JobId</th>
                  <th>JobName</th>
                  <th>Number</th>
                </tr>
              </thead>{job._2.map { s =>
              <tr>
                <td>
                  {s._1._1}
                </td>
                <td>
                  {s._1._2}
                </td>
                <td>
                  {s._2}
                </td>
              </tr>
            }}
            </table>
              <br/>
              <br/>
        }
        else if (job._1.equalsIgnoreCase(CrawlerTaskTraceListener.TASK_TOTAL_NUM)) {
          <h2>Total Tasks Number</h2>
            <table class="bordered">
              <thead>
                <tr>
                  <th>JobId</th>
                  <th>JobName</th>
                  <th>Number</th>
                </tr>
              </thead>{job._2.map { s =>
              <tr>
                <td>
                  {s._1._1}
                </td>
                <td>
                  {s._1._2}
                </td>
                <td>
                  {s._2}
                </td>
              </tr>
            }}
            </table>
        }
      }}
        <div></div>
      </div>
    }
    assemblePage(showPage, "crawler task trace")
  }

  override def renderJson(request: HttpServletRequest): JValue = Json4sHelp.writeTest

  def cTable(job: (String, mutable.HashMap[(String, String), Int])): Seq[Node] = {
    <table class="bordered">
      <thead>
        <tr>
          <th>JobId</th>
          <th>JobName</th>
          <th>Number</th>
        </tr>
      </thead>{job._2.map { s =>
      <tr>
        <td>
          {s._1._1}
        </td>
        <td>
          {s._1._2}
        </td>
        <td>
          {s._2}
        </td>
      </tr>
    }}
    </table>
  }
}

object ControlWebPage {
  val jobInfoCache: scala.collection.mutable.Map[String, mutable.HashMap[(String, String), Int]] = new scala.collection.mutable.HashMap[String, mutable.HashMap[(String, String), Int]]()
}