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
package org.openrepose.filters.munging

import java.net.URL
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.munging.config.{ChangeDetails, MungingConfig, Patch}

import scala.collection.JavaConverters._

/**
  * Created by adrian on 4/29/16.
  */
class MungingFilter(configurationService: ConfigurationService) extends AbstractConfiguredFilter[MungingConfig](configurationService) {
  override val DEFAULT_CONFIG: String = "munging.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/schema/config/munging.xsd"

  override def doWork(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = ???

  def filterChanges(request: HttpServletRequest): List[ChangeDetails] = {
    val urlPath: String = new URL(request.getRequestURL.toString).getPath
    configuration.getChange.asScala.toList
        .filter(_.getPath.r.findFirstIn(urlPath).isDefined)
        .filter(change => {
          Option(change.getHeaderFilter) match {
            case Some(headerFilter) =>
              Option(request.getHeaders(headerFilter.getName)) match {
                case Some(values) => values.asScala.exists(headerFilter.getValue.r.findFirstIn(_).isDefined)
                case None => false
              }
            case None => true
          }
        })
  }

  def filterRequestChanges(changes: List[ChangeDetails]): List[Patch] = {
    changes.flatMap(change => Option(change.getRequest()))
  }

  def filterResponseChanges(changes: List[ChangeDetails]): List[Patch] = {
    changes.flatMap(change => Option(change.getResponse()))
  }
}
