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
    fd.setFilterAction(FilterAction.PROCESS_RESPONSE)

    Option(filterConfig.getRequest).foreach { requestConfig =>
      requestConfig.getHeader.toList.foreach { name =>
        if (request.getHeaderNames.toList.exists(_.equalsIgnoreCase(name))) {
          val headerValue = request.getHeaders(name).toList.mkString(", ")
          logger.debug(s"REQUEST: merging header $name to $headerValue")
          fd.requestHeaderManager().putHeader(name, headerValue)
        }
      }
    }

    fd
  }

  override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val fd = new FilterDirectorImpl()
    fd.setFilterAction(FilterAction.PASS)
    //This is the stupidest thing I've ever seen in my entire life
    fd.setResponseStatusCode(response.getStatus)

    Option(filterConfig.getResponse).foreach { responseConfig =>
      responseConfig.getHeader.toList.foreach { name =>
        if (response.getHeaderNames.toList.exists(_.equalsIgnoreCase(name))) {
          val headerValue = response.getHeaders(name).toList.mkString(", ")
          logger.debug(s"RESPONSE: merging header $name to $headerValue")
          fd.responseHeaderManager().putHeader(name, headerValue)
        }
      }
    }

    fd
  }
}
