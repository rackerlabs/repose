package org.openrepose.filters.mergeheader

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl

class MergeHeaderHandler(filterConfig: MergeHeaderConfig) extends AbstractFilterLogicHandler with LazyLogging {

  import scala.collection.JavaConversions._

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val fd = new FilterDirectorImpl()
    fd.setFilterAction(FilterAction.PASS)

    Option(filterConfig.getRequest).foreach { requestConfig =>
      requestConfig.getHeader.toList.foreach { name =>
        val headerValue = request.getHeader(name)
        logger.debug(s"REQUEST: Putting $name to $headerValue")
        fd.requestHeaderManager().putHeader(name, headerValue)
      }
    }

    fd
  }

  override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val fd = new FilterDirectorImpl()
    fd.setFilterAction(FilterAction.PASS)

    Option(filterConfig.getResponse).foreach {responseConfig =>
      responseConfig.getHeader.toList.foreach { name =>
        val headerValue = request.getHeader(name)
        logger.debug(s"RESPONSE: Putting $name to $headerValue")
        fd.responseHeaderManager().putHeader(name, headerValue)
      }
    }

    fd
  }
}
