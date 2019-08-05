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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.StrictLogging
import gnieh.diffson.playJson._
import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.commons.utils.string.RegexStringOperators
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodypatcher.config.{BodyPatcherConfig, ChangeDetails, Patch}
import play.api.libs.json.{JsValue, Json => PJson}

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

/**
  * Created by adrian on 4/29/16.
  */
@Named
class BodyPatcherFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[BodyPatcherConfig](configurationService) with RegexStringOperators with StrictLogging {
  import BodyPatcherFilter._

  override val DEFAULT_CONFIG: String = "body-patcher.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/schema/config/body-patcher.xsd"

  override def doWork(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain): Unit = {
    val pathChanges: List[ChangeDetails] = filterPathChanges(httpRequest)
    val requestPatches: List[Patch] = filterRequestChanges(pathChanges)
    val responsePatches: List[Patch] = filterResponseChanges(pathChanges)

    //patch the request
    val attemptedPatch: Try[HttpServletRequest] = Try(determineContentType(httpRequest.getContentType) match {
      case Json =>
        val jsonPatches: List[String] = filterJsonPatches(requestPatches)
        if (jsonPatches.nonEmpty) {
          logger.debug("Applying json patches on the request")
          val originalValue: JsValue = Try(PJson.parse(httpRequest.getInputStream))
            .recover({
              case jpe: JsonParseException =>
                logger.debug("Bad Json body", jpe)
                throw BodyUnparseableException("Couldn't parse the body as json", jpe)
            }).get
          val patchedValue: JsValue = applyJsonPatches(originalValue, jsonPatches)
          new HttpServletRequestWrapper(httpRequest, new ServletInputStreamWrapper(new ByteArrayInputStream(PJson.stringify(patchedValue).getBytes)))
        } else {
          httpRequest
        }
      case Xml =>
        //todo: implement xml support
        httpRequest
      case Other =>
        httpRequest
    })

    attemptedPatch match {
      case Success(patchedRequest) =>
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

                val contentValue = Try(PJson.parse(wrappedResponse.getOutputStreamAsInputStream))
                  .recover({
                    case jpe: JsonParseException =>
                      logger.debug("Bad Json body", jpe)
                      throw BodyUnparseableException("Couldn't parse the body as json", jpe)
                  }).get
                val patchedValue: JsValue = applyJsonPatches(contentValue, jsonPatches)
                wrappedResponse.setOutput(new ByteArrayInputStream(PJson.stringify(patchedValue).getBytes))
              }
            case Xml => //todo implement xml support
            case Other =>
          }
          wrappedResponse.commitToResponse()
        }
      case Failure(rbue: BodyUnparseableException) =>
        httpResponse.sendError(SC_BAD_REQUEST, "Body was unparseable as specified content type")
      case Failure(e) =>
        throw e
    }
  }

  def filterPathChanges(request: HttpServletRequest): List[ChangeDetails] = {
    val path: String = URLDecoder.decode(request.getRequestURI, StandardCharsets.UTF_8.toString)
    configuration.getChange.asScala.toList
        .filter(_.getPath =~ path)
  }

  def filterRequestChanges(changes: List[ChangeDetails]): List[Patch] = {
    changes.flatMap(change => Option(change.getRequest))
  }

  def filterResponseChanges(changes: List[ChangeDetails]): List[Patch] = {
    changes.flatMap(change => Option(change.getResponse))
  }

  def determineContentType(contentType: String): ContentType = {
    Option(contentType).getOrElse("").toLowerCase match {
      case Json.regex() => Json
      case Xml.regex() => Xml
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

object BodyPatcherFilter {
  case class BodyUnparseableException(message: String, cause: Throwable) extends Exception(message, cause)

  sealed trait ContentType { val regex: Regex }
  case object Json extends ContentType { override val regex: Regex = ".*json.*".r }
  case object Xml extends ContentType { override val regex: Regex = ".*xml.*".r }
  case object Other extends ContentType { override val regex: Regex = ".*".r }
}
