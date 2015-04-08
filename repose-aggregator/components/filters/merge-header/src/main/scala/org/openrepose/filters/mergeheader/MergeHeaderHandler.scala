package org.openrepose.filters.mergeheader

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler

class MergeHeaderHandler(filterConfig: MergeHeaderConfig) extends AbstractFilterLogicHandler with LazyLogging {

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    ???
  }

  override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    ???
  }
}
