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

import java.time.Instant

import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import org.eclipse.jetty.server.{Request, RequestLog, Response}
import org.openrepose.commons.utils.logging.HttpLoggingContextHelper
import org.openrepose.core.services.httplogging.HttpLoggingService

/**
  * Closes a context with the [[HttpLoggingService]] for every request received
  * by the [[org.eclipse.jetty.server.HttpChannel]] after the HTTP interaction
  * initiated by the request has completed.
  *
  * The [[RequestLog]] interface is used in place of the
  * [[org.eclipse.jetty.server.HttpChannel.Listener]] interface since
  * [[RequestLog#log]] passes the [[Request]] and [[Response]] objects rather than
  * just the [[Request]] object. [[RequestLog#log]] is called just before the
  * [[org.eclipse.jetty.server.HttpChannel.Listener#onComplete]].
  */
@Named
class HttpLoggingServiceRequestLog @Inject()(httpLoggingService: HttpLoggingService)
  extends RequestLog with StrictLogging {

  override def log(request: Request, response: Response): Unit = {
    Option(HttpLoggingContextHelper.extractFromRequest(request)).foreach { loggingContext =>
      loggingContext.setTimeRequestCompleted(Instant.now)
      loggingContext.setOutboundResponse(response)
      loggingContext.setOutboundResponseStatusCode(response.getCommittedMetaData.getStatus)
      loggingContext.setOutboundResponseReasonPhrase(response.getCommittedMetaData.getReason)
      loggingContext.setOutboundResponseBytesWritten(response.getHttpChannel.getBytesWritten)

      val responseContentLength = response.getCommittedMetaData.getContentLength
      if (responseContentLength != Long.MinValue && responseContentLength != -1) {
        loggingContext.setOutboundResponseContentLength(responseContentLength)
      }

      logger.trace("Added the outbound response {} to the HTTP Logging Service context {}", response, s"${loggingContext.hashCode()}")

      httpLoggingService.close(loggingContext)
      logger.trace("Closed the HTTP Logging Service context {} for {}", s"${loggingContext.hashCode()}", request)
    }
  }
}
