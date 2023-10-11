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
import io.opentracing.{Scope, ScopeManager, SpanContext, Tracer}

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

// todo: Consider making this a trait in later versions, obviously after the users have been replaced with scala
// todo: or do the scala test thing and make a companion object that implements it for those still in java
object ScopeHelper {

  def startSpan(req: HttpServletRequest, tracer: Tracer, logger: Logger, spanKind: String, reposeVersion: String, uriRedactionService: UriRedactionService): Scope = {
    logger.trace("Let's see if there were any OpenTracing spans passed-in")
    val context: Option[SpanContext] =
      Try(tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpRequestCarrier(req))) match {
        case s: Success[SpanContext] => s.toOption
        case Failure(exception) =>
          logger.error("{} {} {}",
            "Incoming tracer could not be parsed.",
            "Starting a new root span even though this is most likely part of a larger span.",
            "Check out the following exception for more details:",
            exception)
          None
      }

    logger.debug("The span context obtained from the request: {}", context.getOrElse("NONE"))
    val span = tracer.buildSpan(s"${req.getMethod} ${uriRedactionService.redact(req.getRequestURI)}")
      .asChildOf(context.orNull)
      .withTag(Tags.SPAN_KIND.getKey, spanKind)
      .withTag(ReposeTags.ReposeVersion, reposeVersion)
      .start()
    val scope = tracer.activateSpan(span)

    logger.debug("New span: {}", span)
    scope
  }

  def closeSpan(res: HttpServletResponse, scopeManager: ScopeManager, scope: Scope): Unit = {
    scopeManager.activeSpan().setTag(Tags.HTTP_STATUS.getKey, res.getStatus)
    scope.close()
  }
}
