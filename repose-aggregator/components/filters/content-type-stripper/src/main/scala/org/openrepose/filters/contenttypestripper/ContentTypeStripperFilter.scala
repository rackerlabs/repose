package org.openrepose.filters.contenttypestripper

import javax.servlet._
import javax.servlet.http.HttpServletRequest

import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpHeaders

import scala.collection.JavaConversions.enumerationAsScalaIterator

class ContentTypeStripperFilter extends Filter {

  override def init(p1: FilterConfig): Unit = {}

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val pushBackRequest = new PushBackHttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
    if(pushBackRequest.getHeaderNames exists ( _.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE))) {
      val byteArray:Array[Byte] = new Array[Byte](8)
      val bytesRead: Int = pushBackRequest.pushbackInputStream.read(byteArray, 0, 8)
      if (bytesRead == -1 || StringUtils.isBlank(new String(byteArray).substring(0,bytesRead))) {
        pushBackRequest.addToHeaderBlacklist(HttpHeaders.CONTENT_TYPE)
      }
      pushBackRequest.pushbackInputStream.unread(byteArray.slice(0,bytesRead))
    }
    chain.doFilter(pushBackRequest, response)
  }

  override def destroy(): Unit = {}
}
