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
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import gnieh.diffson.JsonPatch
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.munging.config.{ChangeDetails, MungingConfig, Patch}
import spray.json.JsValue

import scala.collection.JavaConverters._

/**
  * Created by adrian on 4/29/16.
  */
@Named
class MungingFilter @Inject()(configurationService: ConfigurationService) extends AbstractConfiguredFilter[MungingConfig](configurationService) {
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
    changes.flatMap(change => Option(change.getRequest))
  }

  def filterResponseChanges(changes: List[ChangeDetails]): List[Patch] = {
    changes.flatMap(change => Option(change.getResponse))
  }

  def determineContentType(contentType: String): ContentType = {
    //magic code from stack overflow, i'm a terrible person, http://stackoverflow.com/questions/4636610/how-to-pattern-match-using-regular-expression-in-scala
    implicit class Regex(sc: StringContext) {
      def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
    }

    Option(contentType).getOrElse("").toLowerCase match {
      case r".*json.*" => Json
      case r".*xml.*" => Xml
      case _ => Other
    }
  }

  def filterJsonPatches(patches: List[Patch]): List[String] = {
    patches.flatMap(patch => Option(patch.getJson))
  }

  //todo: fix the imports when we move over to repose 8 and get scala 2.11 and can use play instead of spray
  def applyJsonPatches(body: JsValue, patches: List[String]): JsValue = {
    patches.foldLeft(body)((content: JsValue, patch: String) => {
      val jsPatch: JsonPatch = JsonPatch.parse(patch)
      jsPatch(content)
    })
  }
}

sealed trait ContentType
case object Json extends ContentType
case object Xml extends ContentType
case object Other extends ContentType
