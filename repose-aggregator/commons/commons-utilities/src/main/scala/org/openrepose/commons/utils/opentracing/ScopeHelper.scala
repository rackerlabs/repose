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
package org.openrepose.commons.utils.opentracing

import io.opentracing.propagation.Format
import io.opentracing.tag.Tags
import io.opentracing.{Scope, SpanContext, Tracer}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

object ScopeHelper {

  def startSpan(req: HttpServletRequest, tracer: Tracer, logger: Logger): Scope = {
    logger.trace("Let's see if there were any OpenTracing spans passed-in")
    val context: Option[SpanContext] =
      Try(tracer.extract(Format.Builtin.HTTP_HEADERS, new TracerExtractor(req))) match {
        case Success(_) => _
        case Failure(exception) =>
          logger.error("{} {} {}",
            "Incoming tracer could not be parsed.",
            "Starting a new root span even though this is most likely part of a larger span.",
            "Check out the following exception for more details:",
            exception)
          None
      }

    logger.debug("The span context obtained from the request: {}", context.getOrElse("NONE"))
    var spanBuilder = tracer.buildSpan(String.format("%s %s", req.getMethod, req.getRequestURI))
    spanBuilder = context.map(spanContext => spanBuilder.asChildOf(spanContext)).getOrElse(spanBuilder)
    val scope = spanBuilder.withTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_CLIENT).startActive(true)
    logger.debug("New span: {}", scope.span)
    scope
  }

  def closeSpan(res: HttpServletResponse, scope: Scope): Unit = {
    scope.span.setTag(Tags.HTTP_STATUS.getKey, res.getStatus)
    scope.close()
  }
}
