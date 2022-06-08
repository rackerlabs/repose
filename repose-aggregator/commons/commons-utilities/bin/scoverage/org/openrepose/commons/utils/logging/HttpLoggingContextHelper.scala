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
package org.openrepose.commons.utils.logging

import com.typesafe.scalalogging.StrictLogging
import javax.servlet.http.HttpServletRequest
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.core.services.httplogging.HttpLoggingContext

object HttpLoggingContextHelper extends StrictLogging {

  /**
    * @param request the [[HttpServletRequest]] from which to extract a [[HttpLoggingContext]]
    * @return the [[HttpLoggingContext]] for this request, or null if not available
    */
  def extractFromRequest(request: HttpServletRequest): HttpLoggingContext = {
    Option(request.getAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT)) match {
      case Some(context: HttpLoggingContext) =>
        context
      case Some(_) =>
        logger.warn("Could not obtain the HTTP Logging Service context -- context from request {} is invalid", request)
        null
      case None =>
        logger.warn("Could not obtain the HTTP Logging Service context -- context from request {} is missing", request)
        null
    }
  }
}
