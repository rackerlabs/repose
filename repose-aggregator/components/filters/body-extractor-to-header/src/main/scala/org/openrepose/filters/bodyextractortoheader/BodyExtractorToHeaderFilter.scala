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
package org.openrepose.filters.bodyextractortoheader

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import com.jayway.jsonpath.{DocumentContext, JsonPath, Configuration => JsonConfiguration, Option => JsonOption}
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodyextractortoheader.config.BodyExtractorToHeaderConfig

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

@Named
class BodyExtractorToHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[BodyExtractorToHeaderConfig](configurationService) {
  override val DEFAULT_CONFIG: String = "body-extractor-to-header.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/schema/config/body-extractor-to-header.xsd"

  private var extractions: Iterable[Extraction] = _
  private val jsonPathConfiguration = JsonConfiguration.defaultConfiguration()
  jsonPathConfiguration.addOptions(JsonOption.DEFAULT_PATH_LEAF_TO_NULL)

  override def doWork(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val mutableHttpRequest = MutableHttpServletRequest.wrap(request.asInstanceOf[HttpServletRequest])

    def addHeader(name: String, value: String, quality: Option[java.lang.Double], overwrite: Boolean): Unit = {
      if (overwrite) {
        mutableHttpRequest.removeHeader(name)
      }
      ////////////////////////////////////////////////////////////////////////////////
      // @TODO: Replace this with the new wrapper way on repose 8 branch.
      quality match {
        case Some(qual) => mutableHttpRequest.addHeader(name, s"$value;q=$qual")
        //case Some(qual) => mutableHttpRequest.addHeader(name, value, qual)
        case None => mutableHttpRequest.addHeader(name, value)
      }
      ////////////////////////////////////////////////////////////////////////////////
    }

    val jsonDoc: Option[Try[DocumentContext]] = {
      Option(mutableHttpRequest.getContentType) filter { contentType =>
        contentType.toLowerCase.contains("json")
      } map { contentType =>
        ////////////////////////////////////////////////////////////////////////////////
        // @TODO: Update this to wrap the stream if it doesn't support mark/reset when on repose 8 branch.
        // see: https://github.com/rackerlabs/repose/blob/REP-3843_BodyExtractorToHeader/repose-aggregator/commons/utilities/src/main/java/org/openrepose/commons/utils/servlet/http/MutableHttpServletRequest.java#L124-L125
        val is = mutableHttpRequest.getInputStream
        if (is.markSupported()) is.mark(Integer.MAX_VALUE)
        val jsonString = Source.fromInputStream(is).mkString
        if (is.markSupported()) is.reset()
        ////////////////////////////////////////////////////////////////////////////////
        Try(JsonPath.using(jsonPathConfiguration).parse(jsonString))
      }
    }

    extractions foreach { extraction =>
      jsonDoc match {
        case Some(Success(doc)) =>
          val extracted = Try(doc.read[Any](extraction.jsonPath))

          (extracted, extraction.defaultValue, extraction.nullValue) match {
            // JSONPath value was extracted AND is NOT Null
            case (Success(headerValue), _, _) if Option(headerValue).isDefined =>
              addHeader(extraction.headerName, headerValue.toString, extraction.quality, extraction.overwrite)
            // JSONPath value was extracted AND is Null AND NullValue is defined
            case (Success(headerValue), _, Some(nullValue)) =>
              addHeader(extraction.headerName, nullValue, extraction.quality, extraction.overwrite)
            // JSONPath value was NOT extracted AND DefaultValue is defined
            case (Failure(e), Some(defaultValue), _) =>
              addHeader(extraction.headerName, defaultValue, extraction.quality, extraction.overwrite)
            case (_, _, _) => // don't add a header
          }
        case _ => // do nothing
      }
    }

    chain.doFilter(mutableHttpRequest, response)
  }

  override def configurationUpdated(configurationObject: BodyExtractorToHeaderConfig): Unit = {
    extractions = configurationObject.getExtraction.asScala map {
      extraction => Extraction(extraction.getHeader,
        extraction.getJsonpath,
        Option(extraction.getDefaultIfMiss),
        Option(extraction.getDefaultIfNull),
        extraction.isOverwrite,
        Option(extraction.getQuality))
    }
    super.configurationUpdated(configurationObject)
  }

  case class Extraction(headerName: String,
                        jsonPath: String,
                        defaultValue: Option[String],
                        nullValue: Option[String],
                        overwrite: Boolean,
                        quality: Option[java.lang.Double])

}
