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
package org.openrepose.filters.bodypatcher

import java.net.URL
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import org.openrepose.core.filter.AbstractConfiguredFilter
import gnieh.diffson.playJson._
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodypatcher.config.ChangeDetails
import org.openrepose.filters.bodypatcher.config.BodyPatcherConfig
import org.openrepose.filters.bodypatcher.config.Patch
import play.api.libs.json.JsValue

import scala.collection.JavaConverters._

/**
  * Created by adrian on 4/29/16.
  */
class BodyPatcherFilter(configurationService: ConfigurationService) extends AbstractConfiguredFilter[BodyPatcherConfig](configurationService) {
  override val DEFAULT_CONFIG: String = "body-patcher.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/schema/config/body-patcher.xsd"

  override def doWork(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = ???

  def filterChanges(request: HttpServletRequest): List[ChangeDetails] = {
    val urlPath: String = new URL(request.getRequestURL.toString).getPath
    configuration.getChange.asScala.toList
        .filter(_.getPath.r.findFirstIn(urlPath).isDefined)
  }

  def filterRequestChanges(changes: List[ChangeDetails]): List[Patch] = {
    changes.flatMap(change => Option(change.getRequest))
  }

  def filterResponseChanges(changes: List[ChangeDetails]): List[Patch] = {
    changes.flatMap(change => Option(change.getResponse))
  }

  def filterJsonPatches(patches: List[Patch]): List[String] = {
    patches.flatMap(patch => Option(patch.getJson))
  }

  def applyJsonPatches(body: JsValue, patches: List[String]): JsValue = {
    patches.foldLeft(body)((content: JsValue, patch: String) => {
      val jsPatch: JsonPatch = JsonPatch.parse(patch)
      jsPatch(content)
    })
  }
}
