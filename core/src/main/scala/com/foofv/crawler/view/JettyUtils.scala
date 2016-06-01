package com.foofv.crawler.view

import java.net.{BindException, InetSocketAddress, URL}
import javax.servlet.DispatcherType
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.foofv.crawler.{CrawlerException, CrawlerConf}
import com.foofv.crawler.util.{Util, Logging}
import org.eclipse.jetty.util.MultiException

import scala.language.implicitConversions
import scala.xml.Node

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler._
import org.eclipse.jetty.servlet._
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.{pretty, render}


/**
 * @author soledede
 */
private[crawler] object JettyUtil extends Logging {

  // Base type for a function that returns something based on an HTTP request. Allows for
  // implicit conversion from many types of functions to jetty Handlers.
  type Responder[T] = HttpServletRequest => T

  class ServletParams[T <% AnyRef](val responder: Responder[T],
                                   val contentType: String,
                                   val extractFn: T => String = (in: Any) => in.toString) {}

  // Conversions from various types of Responder's to appropriate servlet parameters
  implicit def jsonResponderToServlet(responder: Responder[JValue]): ServletParams[JValue] =
    new ServletParams(responder, "text/json", (in: JValue) => pretty(render(in)))

  //(("") /: in)(_ + _))  in.foldLeft("")(_+_))  in.foldLeft("")((x, y) => x + y)
 // (("") /: in)(_ + _))
  implicit def htmlResponderToServlet(responder: Responder[Seq[Node]]): ServletParams[Seq[Node]] =
    new ServletParams(responder, "text/html", (in: Seq[Node]) => "<!DOCTYPE html>" + (("") /: in)(_ + _))

  implicit def textResponderToServlet(responder: Responder[String]): ServletParams[String] =
    new ServletParams(responder, "text/plain")

  def createServlet[T <% AnyRef](
                                  servletParams: ServletParams[T]): HttpServlet = {
    new HttpServlet {
      override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
        try {
          response.setContentType("%s;charset=utf-8".format(servletParams.contentType))
          response.setStatus(HttpServletResponse.SC_OK)
          val result = servletParams.responder(request)
          response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
          // scalastyle:off println
          response.getWriter.println(servletParams.extractFn(result))
          // scalastyle:on println

        } catch {
          case e: IllegalArgumentException =>
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage)
          case e: Exception =>
            logWarning(s"GET ${request.getRequestURI} failed: $e", e)
            throw e
        }
      }

      protected override def doTrace(req: HttpServletRequest, res: HttpServletResponse): Unit = {
        res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
      }
    }
  }

  /** Create a context handler that responds to a request with the given path prefix */
  def createServletHandler[T <% AnyRef](
                                         path: String,
                                         servletParams: ServletParams[T],
                                         basePath: String = ""): ServletContextHandler = {
    createServletHandler(path, createServlet(servletParams), basePath)
  }

  /** Create a context handler that responds to a request with the given path prefix */
  def createServletHandler(
                            path: String,
                            servlet: HttpServlet,
                            basePath: String): ServletContextHandler = {
    val prefixedPath = attachPrefix(basePath, path)
    val contextHandler = new ServletContextHandler
    val holder = new ServletHolder(servlet)
    contextHandler.setContextPath(prefixedPath)
    contextHandler.addServlet(holder, "/")
    contextHandler
  }

  /** Create a handler that always redirects the user to the given path */
  def createRedirectHandler(
                             srcPath: String,
                             destPath: String,
                             beforeRedirect: HttpServletRequest => Unit = x => (),
                             basePath: String = "",
                             httpMethods: Set[String] = Set("GET")): ServletContextHandler = {
    val prefixedDestPath = attachPrefix(basePath, destPath)
    val servlet = new HttpServlet {
      override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
        if (httpMethods.contains("GET")) {
          doRequest(request, response)
        } else {
          response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
        }
      }

      override def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
        if (httpMethods.contains("POST")) {
          doRequest(request, response)
        } else {
          response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
        }
      }

      private def doRequest(request: HttpServletRequest, response: HttpServletResponse): Unit = {
        beforeRedirect(request)
        // Make sure we don't end up with "//" in the middle
        val newUrl = new URL(new URL(request.getRequestURL.toString), prefixedDestPath).toString
        response.sendRedirect(newUrl)
      }

      protected override def doTrace(req: HttpServletRequest, res: HttpServletResponse): Unit = {
        res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
      }
    }
    createServletHandler(srcPath, servlet, basePath)
  }

  /** Create a handler for serving files from a static directory */
  def createStaticHandler(resourceBase: String, path: String): ServletContextHandler = {
    val contextHandler = new ServletContextHandler
    contextHandler.setInitParameter("org.eclipse.jetty.servlet.Default.gzip", "false")
    val staticHandler = new DefaultServlet
    val holder = new ServletHolder(staticHandler)
    Option(Util.getClassLoader.getResource(resourceBase)) match {
      case Some(res) =>
        holder.setInitParameter("resourceBase", res.toString)
      case None =>
        throw new Exception("Could not find resource path for Web UI: " + resourceBase)
    }
    contextHandler.setContextPath(path)
    contextHandler.addServlet(holder, "/")
    contextHandler
  }

  /** Add filters, if any, to the given list of ServletContextHandlers */
  def addFilters(handlers: Seq[ServletContextHandler], conf: CrawlerConf) {
    val filters: Array[String] = conf.get("crawler.ui.filters", "").split(',').map(_.trim())
    filters.foreach {
      case filter: String =>
        if (!filter.isEmpty) {
          logInfo("Adding filter: " + filter)
          val holder: FilterHolder = new FilterHolder()
          holder.setClassName(filter)
          // Get any parameters for each filter
          conf.get("crawler." + filter + ".params", "").split(',').map(_.trim()).toSet.foreach {
            param: String =>
              if (!param.isEmpty) {
                val parts = param.split("=")
                if (parts.length == 2) holder.setInitParameter(parts(0), parts(1))
              }
          }

          val prefix = s"crawler.$filter.param."
          conf.getAll
            .filter { case (k, v) => k.length() > prefix.length() && k.startsWith(prefix) }
            .foreach { case (k, v) => holder.setInitParameter(k.substring(prefix.length()), v) }

          val enumDispatcher = java.util.EnumSet.of(DispatcherType.ASYNC, DispatcherType.ERROR,
            DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST)
          handlers.foreach { case (handler) => handler.addFilter(holder, "/*", enumDispatcher) }
        }
    }
  }

  /**
   * Attempt to start a Jetty server bound to the supplied hostName:port using the given
   * context handlers.
   *
   * If the desired port number is contended, continues incrementing ports until a free port is
   * found. Return the jetty Server object, the chosen port, and a mutable collection of handlers.
   */
  def startJettyServer(
                        hostName: String,
                        port: Int,
                        handlers: Seq[ServletContextHandler],
                        conf: CrawlerConf,
                        serverName: String = ""): ServerInfo = {

    addFilters(handlers, conf)

    val collection = new ContextHandlerCollection
    val gzipHandlers = handlers.map { h =>
      val gzipHandler = new GzipHandler
      gzipHandler.setHandler(h)
      gzipHandler
    }
    collection.setHandlers(gzipHandlers.toArray)

    // Bind to the given port, or throw a java.net.BindException if the port is occupied
    def connect(currentPort: Int): (Server, Int) = {
      val server = new Server(new InetSocketAddress(hostName, currentPort))
      val pool = new QueuedThreadPool
      pool.setDaemon(true)
      server.setThreadPool(pool)
      val errorHandler = new ErrorHandler()
      errorHandler.setShowStacks(true)
      server.addBean(errorHandler)
      server.setHandler(collection)
      try {
        server.start()
        (server, server.getConnectors.head.getLocalPort)
      } catch {
        case e: Exception =>
          server.stop()
          pool.stop()
          throw e
      }
    }

    val (server, boundPort) = startServiceOnPort[Server](port, connect, conf, serverName)
    ServerInfo(server, boundPort, collection)
  }

  /** Attach a prefix to the given path, but avoid returning an empty path */
  private def attachPrefix(basePath: String, relativePath: String): String = {
    if (basePath == "") relativePath else (basePath + relativePath).stripSuffix("/")
  }


  /**
   * Attempt to start a service on the given port, or fail after a number of attempts.
   * Each subsequent attempt uses 1 + the port used in the previous attempt (unless the port is 0).
   *
   * @param startPort The initial port to start the service on.
   * @param startService Function to start service on a given port.
   *                     This is expected to throw java.net.BindException on port collision.
   * @param conf A CrawlerConf used to get the maximum number of retries when binding to a port.
   * @param serviceName Name of the service.
   */
  def startServiceOnPort[T](
                             startPort: Int,
                             startService: Int => (T, Int),
                             conf: CrawlerConf,
                             serviceName: String = ""): (T, Int) = {

    require(startPort == 0 || (1024 <= startPort && startPort < 65536),
      "startPort should be between 1024 and 65535 (inclusive), or 0 for a random free port.")

    val serviceString = if (serviceName.isEmpty) "" else s" '$serviceName'"
    val maxRetries = portMaxRetries(conf)
    for (offset <- 0 to maxRetries) {
      // Do not increment port if startPort is 0, which is treated as a special port
      val tryPort = if (startPort == 0) {
        startPort
      } else {
        // If the new port wraps around, do not try a privilege port
        ((startPort + offset - 1024) % (65536 - 1024)) + 1024
      }
      try {
        val (service, port) = startService(tryPort)
        logInfo(s"Successfully started service$serviceString on port $port.")
        return (service, port)
      } catch {
        case e: Exception if isBindCollision(e) =>
          if (offset >= maxRetries) {
            val exceptionMessage =
              s"${e.getMessage}: Service$serviceString failed after $maxRetries retries!"
            val exception = new BindException(exceptionMessage)
            // restore original stack trace
            exception.setStackTrace(e.getStackTrace)
            throw exception
          }
          logWarning(s"Service$serviceString could not bind on port $tryPort. " +
            s"Attempting port ${tryPort + 1}.")
      }
    }
    // Should never happen
    throw new CrawlerException(s"Failed to start service$serviceString on port $startPort")
  }

  /**
   * Maximum number of retries when binding to a port before giving up.
   */
  def portMaxRetries(conf: CrawlerConf): Int = {
    val maxRetries = conf.getOption("crawler.port.maxRetries").map(_.toInt)
    if (conf.contains("crawler.testing")) {
      // Set a higher number of retries for tests...
      maxRetries.getOrElse(100)
    } else {
      maxRetries.getOrElse(16)
    }
  }

  /**
   * Return whether the exception is caused by an address-port collision when binding.
   */
  def isBindCollision(exception: Throwable): Boolean = {
    exception match {
      case e: BindException =>
        if (e.getMessage != null) {
          return true
        }
        isBindCollision(e.getCause)
      case e: MultiException => {
        logError("bind port faield", e.getCause)
        false
      }
      case e: Exception => isBindCollision(e.getCause)
      case _ => false
    }
  }

}

private[crawler] case class ServerInfo(
                                        server: Server,
                                        boundPort: Int,
                                        rootHandler: ContextHandlerCollection)
