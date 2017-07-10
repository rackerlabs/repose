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
package org.openrepose.filters.attributemapping

import java.io.{BufferedInputStream, InputStream}
import javax.inject.Named
import javax.servlet._
import javax.servlet.http.HttpServletResponse.{SC_BAD_REQUEST, SC_UNSUPPORTED_MEDIA_TYPE}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.transform.TransformerException
import javax.xml.transform.stream.StreamSource

import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}
import com.rackspace.identity.components.{AttributeMapper, XSDEngine}
import com.typesafe.scalalogging.slf4j.LazyLogging
import net.sf.saxon.s9api.SaxonApiException
import org.apache.commons.io.input.CloseShieldInputStream
import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper

import scala.util.{Failure, Success, Try}

@Named
class AttributeMappingPolicyValidationFilter extends Filter with LazyLogging {

  import AttributeMappingPolicyValidationFilter._

  // Magic
  private val transformerFactory = new net.sf.saxon.TransformerFactoryImpl

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpServletRequest = request.asInstanceOf[HttpServletRequest]
    val httpServletResponse = response.asInstanceOf[HttpServletResponse]
    val requestContentType = httpServletRequest.getContentType

    validateHttpMethod(httpServletRequest) flatMap { request =>
      validatePolicy(requestContentType, request.getInputStream)
    } map { stream =>
      new ServletInputStreamWrapper(stream)
    } match {
      case Success(normalizedPolicyInputStream) =>
        chain.doFilter(
          new HttpServletRequestWrapper(
            httpServletRequest,
            normalizedPolicyInputStream),
          response)
      case Failure(exception) =>
        exception match {
          case uhme: UnsupportedHttpMethodException =>
            logger.debug("Unsupported HTTP method -- no validation performed", uhme)
            chain.doFilter(request, response)
          case ucte: UnsupportedContentTypeException =>
            logger.debug("Unsupported Content-Type -- validation failed", ucte)
            httpServletResponse.sendError(
              SC_UNSUPPORTED_MEDIA_TYPE,
              ucte.message)
          case e@(_: SaxonApiException | _: TransformerException | _: JsonProcessingException | _: JsonParseException) =>
            logger.debug("Validation failed", e)
            httpServletResponse.sendError(
              SC_BAD_REQUEST,
              "Failed to validate attribute mapping policy in request")
        }
    }
  }

  def validateHttpMethod(request: HttpServletRequest): Try[HttpServletRequest] = {
    if ("PUT".equalsIgnoreCase(request.getMethod)) Success(request)
    else Failure(UnsupportedHttpMethodException(s"${request.getMethod} is not a supported HTTP method"))
  }

  def validatePolicy(contentType: String, policy: InputStream): Try[InputStream] = {
    val contentTypeLowerCase = contentType.toLowerCase
    val bufferedStream = new BufferedInputStream(policy)
    bufferedStream.mark(Integer.MAX_VALUE)
    val requestStreamSource = new StreamSource(new CloseShieldInputStream(bufferedStream))
    val xsdEngineString = XSDEngine.AUTO.toString

    if (contentTypeLowerCase.contains("yaml")) {
      Try {
        // Validate the policy
        AttributeMapper.validatePolicy(
          AttributeMapper.parseYamlNode(requestStreamSource),
          xsdEngineString)

        bufferedStream.reset()
        bufferedStream
      }
    } else {
      Failure(UnsupportedContentTypeException(s"$contentType is not a supported Content-Type"))
    }
  }
}

object AttributeMappingPolicyValidationFilter {

  case class UnsupportedContentTypeException(message: String) extends Exception(message)

  case class UnsupportedHttpMethodException(message: String) extends Exception(message)

}
