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
package org.openrepose.filters.urinormalization.normalizer

import org.openrepose.commons.utils.http.normal.ParameterFilter
import org.openrepose.filters.urinormalization.config.{HttpUriParameterList, UriParameter}

import scala.collection.JavaConversions._

class MultiInstanceWhiteList(val parameterList: HttpUriParameterList) extends ParameterFilter {

  private var instanceMap: Map[String, Long] = Map.empty

  def shouldAccept(name: String): Boolean = {
    Option(parameterList) exists { pl =>
      pl.getParameter.find(parameter => if (parameter.isCaseSensitive) name == parameter.getName else name.equalsIgnoreCase(parameter.getName))
        .exists(logHit)
    }
  }

  private def logHit(parameter: UriParameter): Boolean = {
    if (parameter.getMultiplicity <= 0) {
      true
    } else {
      val hitCount = instanceMap.get(parameter.getName)
      val nextHitCount = hitCount.map(_ + 1L).getOrElse(1L)
      if (nextHitCount <= parameter.getMultiplicity) {
        instanceMap = instanceMap + (parameter.getName -> nextHitCount)
        true
      } else {
        false
      }
    }
  }
}