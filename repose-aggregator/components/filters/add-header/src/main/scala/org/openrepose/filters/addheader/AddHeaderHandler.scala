package org.openrepose.filters.addheader

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector, HeaderManager}
import org.openrepose.filters.addheader.config.{AddHeadersConfig, Header}

import scala.collection.JavaConverters._

class AddHeaderHandler(config: AddHeadersConfig) extends AbstractFilterLogicHandler with LazyLogging {

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val filterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.requestHeaderManager()
    if (config.getResponse != null) {
      filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    } else {
      filterDirector.setFilterAction(FilterAction.PASS)
    }

    modifyHeaders(config.getRequest.getHeader.asScala, headerManager)

    filterDirector
  }

  override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val filterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.responseHeaderManager()
    filterDirector.setFilterAction(FilterAction.PASS)

    modifyHeaders(config.getResponse.getHeader.asScala, headerManager)

    filterDirector
  }

  def modifyHeaders(configuredHeaders: Seq[Header], headerManager: HeaderManager): Unit = {
    configuredHeaders foreach { configuredHeader =>
      if (configuredHeader.isOverwrite) {
        headerManager.removeHeader(configuredHeader.getName)
        logger.debug(s"Header removed: ${configuredHeader.getName}")
      }

      configuredHeader.getValue.asScala foreach { configuredHeaderValue =>
        headerManager.appendHeader(configuredHeader.getName, configuredHeaderValue, configuredHeader.getQuality)
        logger.debug(s"Added header ${configuredHeader.getName} with value $configuredHeaderValue and quality ${configuredHeader.getQuality}")
      }
    }
  }
}