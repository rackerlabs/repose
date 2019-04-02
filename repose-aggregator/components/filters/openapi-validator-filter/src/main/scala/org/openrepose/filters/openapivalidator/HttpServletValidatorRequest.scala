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
package org.openrepose.filters.openapivalidator

import java.io.InputStream
import java.nio.charset.{IllegalCharsetNameException, StandardCharsets, UnsupportedCharsetException}
import java.util
import java.util.{Collections, Optional}

import com.atlassian.oai.validator.model.{Request, SimpleRequest}
import javax.servlet.http.HttpServletRequest
import org.openrepose.commons.utils.http.normal.{QueryParameter, QueryParameterCollection}
import org.openrepose.filters.openapivalidator.HttpServletValidatorRequest._

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * A wrapper that exposes an underlying [[HttpServletRequest]] as a [[Request]]
  * usable by an [[com.atlassian.oai.validator.OpenApiInteractionValidator]].
  */
class HttpServletValidatorRequest(httpServletRequest: HttpServletRequest)
  extends Request {

  private lazy val encoding: String = {
    Option(httpServletRequest.getCharacterEncoding)
      .getOrElse(StandardCharsets.ISO_8859_1.name)
  }
  private lazy val body: String = {
    inputStreamToString(httpServletRequest.getInputStream, encoding)
  }

  // In accordance with OpenAPI specification, only query string parameters (not form data parameters)
  // are made available:
  // https://github.com/OAI/OpenAPI-Specification/blob/3.0.2/versions/3.0.2.md#parameter-locations
  // For an example on validating form data parameters, see:
  // https://github.com/OAI/OpenAPI-Specification/blob/3.0.2/versions/3.0.2.md#operation-object-example
  private lazy val queryParameters: Seq[QueryParameter] = {
    Option(httpServletRequest.getQueryString)
      .map(new QueryParameterCollection(_))
      .map(_.getParameters.asScala)
      .getOrElse(Seq.empty)
  }

  // Leverages the SimpleRequest.Builder to perform additional header processing like value splitting on commas
  // and constructing a key-case-insensitive Map.
  private lazy val headers: util.Map[String, util.Collection[String]] = {
    val requestBuilder = SimpleRequest.Builder.get("/")
    httpServletRequest.getHeaderNames.asScala.foreach { headerName =>
      requestBuilder.withHeader(headerName, Collections.list(httpServletRequest.getHeaders(headerName)))
    }
    requestBuilder.build.getHeaders
  }

  override def getPath: String = {
    httpServletRequest.getRequestURI
  }

  override def getMethod: Request.Method = {
    val method = httpServletRequest.getMethod
    try {
      Request.Method.valueOf(method.toUpperCase)
    } catch {
      case e: IllegalArgumentException =>
        // Since the validation library currently supports the same methods as Repose,
        // this exception is never expected to be thrown.
        throw RequestConversionException(s"'$method' is an unsupported HTTP method", e)
    }
  }

  override def getBody: Optional[String] = {
    try {
      if (body.isEmpty) {
        Optional.empty[String]
      } else {
        Optional.of(body)
      }
    } catch {
      case e@(_: IllegalCharsetNameException | _: UnsupportedCharsetException) =>
        throw RequestConversionException(s"'$encoding' in an unsupported character encoding", e)
    }
  }

  override def getQueryParameters: util.Collection[String] = {
    queryParameters
      .map(_.getName)
      .asJava
  }

  override def getQueryParameterValues(name: String): util.Collection[String] = {
    queryParameters
      .find(_.getName.equals(name))
      .map(_.getValues)
      .getOrElse(Collections.emptyList[String])
  }

  override def getHeaders: util.Map[String, util.Collection[String]] = {
    headers
  }

  override def getHeaderValues(name: String): util.Collection[String] = {
    headers.getOrDefault(name, util.Collections.emptyList[String])
  }
}

object HttpServletValidatorRequest {
  def apply(httpServletRequest: HttpServletRequest): Request = {
    new HttpServletValidatorRequest(httpServletRequest)
  }

  case class RequestConversionException(message: String, cause: Throwable) extends RuntimeException(message, cause)

  /**
    * @param inputStream an [[java.io.InputStream]]
    * @param encoding    a [[String]] naming a [[java.nio.charset.Charset]]
    * @return a [[String]] representation of the data read from `inputStream`
    */
  private def inputStreamToString(inputStream: InputStream, encoding: String): String = {
    Source.fromInputStream(inputStream, encoding)
      .getLines
      .mkString(System.lineSeparator)
  }
}

