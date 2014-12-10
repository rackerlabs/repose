package org.openrepose.filters.addheader

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}
import org.openrepose.filters.addheader.config.HeaderType

import scala.collection.JavaConverters._

class AddHeaderHandler(sourceHeaders: List[HeaderType]) extends AbstractFilterLogicHandler with LazyLogging {

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val director = new FilterDirectorImpl()
    //By default, if nothing happens we're going to pass
    director.setFilterAction(FilterAction.PASS)
    val headerManager = director.requestHeaderManager()

    for (sourceHeader <- sourceHeaders) yield
      for (value <- sourceHeader.getValue.asScala.toList) yield {
        if (sourceHeader.isRemoveOriginal) {
          headerManager.removeHeader(sourceHeader.getName)
          logger.trace(
            s"Header removed: ${sourceHeader.getName} with value $value and quality ${sourceHeader.getQuality}")
        }
        headerManager.appendHeader(sourceHeader.getName, value, sourceHeader.getQuality)
        logger.trace(s"Added header ${sourceHeader.getName} with value $value and quality ${sourceHeader.getQuality}")
      }
    director
  }
}