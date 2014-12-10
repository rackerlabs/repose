package org.openrepose.filters.addheader

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}
import org.openrepose.filters.addheader.config.Header

import scala.collection.JavaConverters._

class AddHeaderHandler(configuredHeaders: List[Header]) extends AbstractFilterLogicHandler with LazyLogging {

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val filterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.requestHeaderManager()
    filterDirector.setFilterAction(FilterAction.PASS)

    configuredHeaders foreach { configuredHeader =>
      if (configuredHeader.isRemoveOriginal) {
        headerManager.removeHeader(configuredHeader.getName)
        logger.trace(s"Header removed: ${configuredHeader.getName}")
      }

      configuredHeader.getValue.asScala foreach { configuredHeaderValue =>
        headerManager.appendHeader(configuredHeader.getName, configuredHeaderValue, configuredHeader.getQuality)
        logger.trace(s"Added header ${configuredHeader.getName} with value $configuredHeaderValue and quality ${configuredHeader.getQuality}")
      }
    }

    filterDirector
  }
}