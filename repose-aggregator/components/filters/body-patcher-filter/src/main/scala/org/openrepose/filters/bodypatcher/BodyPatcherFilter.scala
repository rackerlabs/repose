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

import java.io.ByteArrayInputStream
import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.core.filter.AbstractConfiguredFilter
import gnieh.diffson.playJson._
import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodypatcher.config.ChangeDetails
import org.openrepose.filters.bodypatcher.config.BodyPatcherConfig
import org.openrepose.filters.bodypatcher.config.Patch
import play.api.libs.json.{JsValue, Json => PJson}

import scala.collection.JavaConverters._

/**
  * Created by adrian on 4/29/16.
  */
@Named
class BodyPatcherFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[BodyPatcherConfig](configurationService) with LazyLogging {
  override val DEFAULT_CONFIG: String = "body-patcher.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/schema/config/body-patcher.xsd"

  override def doWork(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpRequest: HttpServletRequest = request.asInstanceOf[HttpServletRequest]
    val httpResponse: HttpServletResponse = response.asInstanceOf[HttpServletResponse]

    val pathChanges: List[ChangeDetails] = filterChanges(httpRequest)
    val requestPatches: List[Patch] = filterRequestChanges(pathChanges)
    val responsePatches: List[Patch] = filterResponseChanges(pathChanges)

    //patch the request
    val patchedRequest: HttpServletRequest = determineContentType(httpRequest.getContentType) match {
      case Json =>
        val jsonPatches: List[String] = filterJsonPatches(requestPatches)
        if (jsonPatches.nonEmpty) {
          logger.debug("Applying json patches on the request")
          val patchedValue: JsValue = applyJsonPatches(PJson.parse(httpRequest.getInputStream), jsonPatches)
          new HttpServletRequestWrapper(httpRequest, new ServletInputStreamWrapper(new ByteArrayInputStream(PJson.stringify(patchedValue).getBytes)))
        } else {
          httpRequest
        }
      case Xml =>
        //todo: implement xml support
        httpRequest
      case Other =>
        httpRequest
    }


    //check if we might do work to the response
    if (responsePatches.isEmpty) {
      chain.doFilter(patchedRequest, httpResponse)
    } else {
      val wrappedResponse: HttpServletResponseWrapper = new HttpServletResponseWrapper(httpResponse, PASSTHROUGH, MUTABLE)
      chain.doFilter(patchedRequest, wrappedResponse)
      wrappedResponse.uncommit()

      determineContentType(wrappedResponse.getContentType) match {
        case Json =>
          val jsonPatches: List[String] = filterJsonPatches(responsePatches)
          if (jsonPatches.nonEmpty) {
            logger.debug("Applying json patches on the response")

            val contentStream = wrappedResponse.getOutputStreamAsInputStream
            val contentValue = if (contentStream.available() > 0) {
              PJson.parse(contentStream)
            } else {
              PJson.obj()
            }
            val patchedValue: JsValue = applyJsonPatches(contentValue, jsonPatches)
            wrappedResponse.setOutput(new ByteArrayInputStream(PJson.stringify(patchedValue).getBytes))
          }
        case Xml => //todo implement xml support
        case Other =>
      }
      wrappedResponse.commitToResponse()
    }
  }

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
