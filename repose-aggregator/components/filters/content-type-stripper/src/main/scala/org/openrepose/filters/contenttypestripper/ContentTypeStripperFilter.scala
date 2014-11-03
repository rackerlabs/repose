package org.openrepose.filters.contenttypestripper

import javax.servlet._
import javax.servlet.http.HttpServletRequest

import org.apache.http.HttpHeaders

import scala.collection.JavaConversions.enumerationAsScalaIterator

class ContentTypeStripperFilter extends Filter {

  override def init(p1: FilterConfig): Unit = {}

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val pushBackRequest = new PushBackHttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
    if(pushBackRequest.getHeaderNames exists ( _.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE))) {
      val byteArray = new Array[Byte](1)
      byteArray(0) = pushBackRequest.pushbackInputStream.read().asInstanceOf[Byte]
      if (byteArray(0) == -1) {
        pushBackRequest.addToHeaderBlacklist(HttpHeaders.CONTENT_TYPE)
      }
      pushBackRequest.pushbackInputStream.unread(byteArray)
    }
    chain.doFilter(pushBackRequest, response)
  }

  override def destroy(): Unit = {}
}
