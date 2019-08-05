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
package org.openrepose.filters.splitheader

import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.openrepose.commons.utils.servlet.http.{HeaderInteractor, HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.splitheader.config.{HeaderList, SplitHeaderConfig}

import scala.collection.JavaConverters._

/**
  * This filter will "split" configured HTTP message headers (defined by header name)
  * across multiple lines, each with a single value.
  *
  * Splitting is performed using the comma character (i.e., ',') as a delimiter.
  *
  * Note: A side effect of this filter is that the resultant header(s) will have
  * casing matching the configured header name rather than the corresponding
  * header name from the HTTP message.
  * Although it is possible to keep the casing of the header name from the HTTP
  * message, we opted not to do so since:
  * 1. By specification, header names are case-insensitive.
  * 2. The added complexity would make the code less clear and succinct,
  * and thus, more difficult to maintain.
  */
@Named
class SplitHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[SplitHeaderConfig](configurationService)
    with StrictLogging {

  override val DEFAULT_CONFIG = "split-header.cfg.xml"
  override val SCHEMA_LOCATION = "/META-INF/schema/config/split-header.xsd"

  override def doWork(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain): Unit = {
    val wrappedHttpRequest = new HttpServletRequestWrapper(httpRequest)
    val wrappedHttpResponse = new HttpServletResponseWrapper(
      httpResponse,
      ResponseMode.MUTABLE,
      ResponseMode.PASSTHROUGH)

    splitHeaders(wrappedHttpRequest, configuration.getRequest)

    chain.doFilter(wrappedHttpRequest, wrappedHttpResponse)

    wrappedHttpResponse.uncommit()

    splitHeaders(wrappedHttpResponse, configuration.getResponse)

    wrappedHttpResponse.commitToResponse()
  }

  private def splitHeaders(wrappedHttpMessage: HeaderInteractor, configuredHeaders: HeaderList): Unit = {
    Option(configuredHeaders).map(_.getHeader.asScala).getOrElse(Seq.empty) foreach { headerToSplit =>
      val headerValues = wrappedHttpMessage.getSplittableHeaders(headerToSplit).asScala
      if (headerValues.size > wrappedHttpMessage.getHeadersList(headerToSplit).size) {
        logger.debug("Splitting header {}", headerToSplit)
        wrappedHttpMessage.removeHeader(headerToSplit)
        headerValues.foreach(wrappedHttpMessage.addHeader(headerToSplit, _))
      }
    }
  }
}
