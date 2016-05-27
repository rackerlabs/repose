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

package org.openrepose.filters.compression

import javax.servlet.{ServletContext, FilterConfig}

import scala.collection.JavaConverters._

class CompressingFilterConfig(config: FilterConfig) extends FilterConfig {
  private var params: Map[String, String] = Map.empty

  wrapFilterConfig()

  private def wrapFilterConfig(): Unit = {
    config.getInitParameterNames.asScala.foreach { name =>
      params = params + (name -> config.getInitParameter(name))
    }
  }

  override def getFilterName: String = config.getFilterName

  override def getServletContext: ServletContext = config.getServletContext

  // returning null because that's what CompressingFilterContext expects for missing keys
  override def getInitParameter(param: String): String = params.getOrElse(param, null)

  override def getInitParameterNames: java.util.Enumeration[String] = params.keysIterator.asJavaEnumeration

  def setInitParameter(key: String, value: String): Unit = params = params + (key -> value)
}
