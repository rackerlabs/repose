/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
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

    Option(config.getRequest) foreach { httpMessage =>
      modifyHeaders(httpMessage.getHeader.asScala, headerManager)
    }

    filterDirector
  }

  override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val filterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.responseHeaderManager()
    filterDirector.setResponseStatusCode(response.getStatus)
    filterDirector.setFilterAction(FilterAction.PASS)

    Option(config.getResponse) foreach { httpMessage =>
      modifyHeaders(httpMessage.getHeader.asScala, headerManager)
    }

    filterDirector
  }

  def modifyHeaders(configuredHeaders: Seq[Header], headerManager: HeaderManager): Unit = {
    configuredHeaders foreach { configuredHeader =>
      if (configuredHeader.isOverwrite) {
        headerManager.removeHeader(configuredHeader.getName)
        logger.debug(s"Header removed: ${configuredHeader.getName}")
      }

      Option(configuredHeader.getQuality) match {
        case Some(quality) =>
          headerManager.appendHeader(configuredHeader.getName, configuredHeader.getValue, quality)
        case None =>
          headerManager.appendHeader(configuredHeader.getName, configuredHeader.getValue)
      }
      logger.debug(s"Added header ${configuredHeader.getName} with value ${configuredHeader.getValue} and quality ${configuredHeader.getQuality}")
    }
  }
}
