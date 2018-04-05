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

import java.util

import io.opentracing.propagation.TextMap
import javax.servlet.http.HttpServletRequest

import scala.Function.tupled
import scala.collection.JavaConverters._

class HttpRequestCarrier(httpServletRequest: HttpServletRequest) extends TextMap {

  val headers: Map[String, List[String]] = Option(httpServletRequest).map(request =>
    request.getHeaderNames.asScala
      .map(headerName => headerName -> request.getHeaders(headerName).asScala.toList)
      .toMap
  ).getOrElse(Map.empty)

  override def put(key: String, value: String): Unit = ???

  override def iterator(): util.Iterator[util.Map.Entry[String, String]] = {
    headers.toIterator
      .flatMap(tupled((key, values) => values.map(new util.AbstractMap.SimpleEntry(key, _))))
      .asJava
      .asInstanceOf[util.Iterator[util.Map.Entry[String, String]]]
  }
}
