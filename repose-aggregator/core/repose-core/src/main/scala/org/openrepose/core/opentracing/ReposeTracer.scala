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
package org.openrepose.core.opentracing

import com.typesafe.scalalogging.StrictLogging
import io.opentracing.noop.{NoopTracer, NoopTracerFactory}
import io.opentracing.propagation.Format
import io.opentracing.util.GlobalTracer
import io.opentracing.{ScopeManager, Span, SpanContext, Tracer}
import javax.annotation.PostConstruct
import javax.inject.Named

/**
  * This is the Global Tracer for Repose and is based on the [[io.opentracing.util.GlobalTracer]].
  *
  * The main differences are this Tracer:
  *  - immediately registers with the OpenTracing GlobalTracer
  *  - allows for registration of a new Tracer at any time.
  */
@Named
class ReposeTracer extends DelegatingTracer with StrictLogging {

  private var tracer: Tracer = NoopTracerFactory.create()

  @PostConstruct
  def init(): Unit = {
    GlobalTracer.register(this)
  }

  override def get(): Tracer =
    tracer

  override def register(newTracer: Tracer): Unit = Option(newTracer) match {
    case Some(someTracer) => tracer = someTracer
    case None => throw new NullPointerException("Cannot register ReposeTracer <null>.")
  }

  override def isRegistered: Boolean =
    !tracer.isInstanceOf[NoopTracer]

  override def scopeManager(): ScopeManager =
    tracer.scopeManager()

  override def buildSpan(operationName: String): Tracer.SpanBuilder =
    tracer.buildSpan(operationName)

  override def inject[C](spanContext: SpanContext, format: Format[C], carrier: C): Unit =
    tracer.inject(spanContext, format, carrier)

  override def extract[C](format: Format[C], carrier: C): SpanContext =
    tracer.extract(format, carrier)

  override def activeSpan(): Span =
    tracer.activeSpan()

  override def toString: String =
    s"${this.getClass.getName.split("\\$").last} {$tracer}"
}
