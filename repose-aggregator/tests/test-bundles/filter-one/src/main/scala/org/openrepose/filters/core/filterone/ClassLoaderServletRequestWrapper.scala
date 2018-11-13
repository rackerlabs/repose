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
package org.openrepose.filters.core.filterone

import java.util

import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}
import org.apache.commons.lang3.StringUtils
import org.openrepose.others.SimplicityDivine

import scala.collection.JavaConverters._

class ClassLoaderServletRequestWrapper(request: HttpServletRequest) extends HttpServletRequestWrapper(request) {
  override def getHeader(headerString: String): String = {
    println("\n\n\n")
    println(s"Requesting header $headerString")
    println("\n\n\n")
    if (StringUtils.startsWith(headerString, "FOO") && (super.getHeader(headerString) != null)) {
      new SimplicityDivine().createBar
    } else {
      super.getHeader(headerString)
    }
  }

  override def getHeaders(headerString: String): util.Enumeration[String] = {
    println("\n\n\n")
    println(s"Requesting headers $headerString")
    println("\n\n\n")
    if (StringUtils.startsWith(headerString, "FOO") && super.getHeaders(headerString).hasMoreElements) {
      Seq(new SimplicityDivine().createBar).toIterator.asJavaEnumeration
    } else {
      super.getHeaders(headerString)
    }
  }
}
