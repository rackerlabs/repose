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
package org.openrepose.filters.urinormalization

import java.util.regex.Pattern

import org.openrepose.commons.utils.http.normal.Normalizer
import org.openrepose.commons.utils.regex.RegexSelector
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.urinormalization.config.HttpMethod

class QueryParameterNormalizer(method: HttpMethod) {

  val uriSelector = new RegexSelector[Normalizer[String]]

  private var lastMatch: Pattern = _

  def getLastMatch: Pattern = lastMatch

  def normalize(request: HttpServletRequestWrapper): Boolean = {
    if (method.name.equalsIgnoreCase(request.getMethod) || method.name.equalsIgnoreCase(HttpMethod.ALL.value)) {
      val result = uriSelector.select(request.getRequestURI)

      if (result.hasKey) {
        val queryStringNormalizer = result.getKey
        request.setQueryString(queryStringNormalizer.normalize(request.getQueryString))
        lastMatch = uriSelector.getLastMatch
        true
      } else {
        false
      }
    } else {
      false
    }
  }
}