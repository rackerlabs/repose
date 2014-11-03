package org.openrepose.filters.contenttypestripper

import java.io.PushbackInputStream
import java.util
import java.util.concurrent.CopyOnWriteArrayList
import javax.servlet.ServletInputStream
import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}

import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper

class PushBackHttpServletRequestWrapper(request: HttpServletRequest) extends HttpServletRequestWrapper(request) {
  private val headerBlacklist: CopyOnWriteArrayList[String] = new CopyOnWriteArrayList[String]
  val pushbackInputStream: PushbackInputStream = new PushbackInputStream(request.getInputStream, 8)

  override def getInputStream: ServletInputStream = new ServletInputStreamWrapper(pushbackInputStream)

  override def getHeaderNames: util.Enumeration[String] = {
    import scala.collection.JavaConversions._
    super.getHeaderNames.collect { case name:String if !headerBlacklist.contains(name.toLowerCase) => name }
  }

  override def getHeader(headerName: String): String = {
    if (headerBlacklist.contains(headerName.toLowerCase)) {
      null
    } else {
      super.getHeader(headerName)
    }
  }

  def addToHeaderBlacklist(headerName: String) = headerBlacklist.add(headerName.toLowerCase)
}