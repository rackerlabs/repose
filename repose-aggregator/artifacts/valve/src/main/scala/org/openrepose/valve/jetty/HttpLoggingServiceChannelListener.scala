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
package org.openrepose.valve.jetty

import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import org.eclipse.jetty.server.{HttpChannel, Request}
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.core.services.httplogging.HttpLoggingService

/**
  * Opens a new context with the [[HttpLoggingService]] for every request that
  * the [[HttpChannel]] receives.
  *
  * The [[HttpChannel.Listener]] interface is used since the [[HttpChannel]]
  * originates the request object and is responsible for invoke the
  * [[org.eclipse.jetty.server.RequestLog]] after the request has been handled.
  * Using this interface enables us to reliably open a logging context as early
  * as possible by leveraging the lifecycle of a request in an [[HttpChannel]].
  * As a result, we should always open a logging context at the correct time.
  */
@Named
class HttpLoggingServiceChannelListener @Inject()(httpLoggingService: HttpLoggingService)
  extends HttpChannel.Listener with StrictLogging {

  override def onRequestBegin(request: Request): Unit = {
    val loggingContext = httpLoggingService.open()
    logger.trace("Opened an HTTP Logging Service context {} for {}", s"${loggingContext.hashCode()}", request)

    loggingContext.setInboundRequest(request)
    logger.trace("Added the inbound request {} to the HTTP Logging Service context {}", request, s"${loggingContext.hashCode()}")

    request.setAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT, loggingContext)
    logger.trace("Added the HTTP Logging Service context {} to the request as an attribute", s"${loggingContext.hashCode()}")
  }
}
