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

import java.io.File
import java.net.URI

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.report.ValidationReport
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.filters.openapivalidator.HttpServletOAIRequest.RequestConversionException
import org.openrepose.filters.openapivalidator.OpenApiValidatorFilter._
import org.openrepose.filters.openapivalidator.config.OpenApiValidatorConfig
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * This filter will validate requests against an OpenAPI document.
  */
@Named
class OpenApiValidatorFilter @Inject()(@Value(ReposeSpringProperties.CORE.CONFIG_ROOT) configurationRoot: String,
                                       configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[OpenApiValidatorConfig](configurationService) with StrictLogging {

  override final val DEFAULT_CONFIG: String = "open-api-validator.cfg.xml"
  override final val SCHEMA_LOCATION: String = "/META-INF/schema/config/open-api-validator.xsd"

  private[openapivalidator] var validator: OpenApiInteractionValidator = _

  override def doWork(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain): Unit = {
    // Ensure that request input stream buffering is supported.
    val bufferingHttpServletRequest = getBufferingHttpServletRequest(httpRequest)

    bufferingHttpServletRequest.getInputStream.mark(Integer.MAX_VALUE)

    logger.trace("Wrapping the servlet request in a validation library request")
    val validationRequest = HttpServletOAIRequest(bufferingHttpServletRequest)

    logger.trace("Validating request")
    val tryValidationReport = Try(validator.validateRequest(validationRequest))

    bufferingHttpServletRequest.getInputStream.reset()

    tryValidationReport match {
      case Success(validationReport) =>
        getPriorityValidationFailure(validationReport) match {
          case Some(validationFailure) =>
            logger.debug("Failed to validate request -- rejecting with status code: '{}' for reason: '{}'", validationFailure.issue.statusCode.toString, validationFailure.message)
            httpResponse.sendError(validationFailure.issue.statusCode, validationFailure.message.getMessage)
          case None =>
            logger.trace("Successfully validated request -- continuing processing")
            chain.doFilter(bufferingHttpServletRequest, httpResponse)
        }
      case Failure(e: RequestConversionException) =>
        logger.error("Failed to convert the servlet request to a validation request", e)
        httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
      case Failure(e) =>
        val statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        logger.error("Failed to validate request -- rejecting with status code: '{}' for reason '{}'", statusCode.toString, e.getMessage, e)
        httpResponse.sendError(statusCode, e.getMessage)
    }
  }

  override def doConfigurationUpdated(newConfiguration: OpenApiValidatorConfig): OpenApiValidatorConfig = {
    validator = OpenApiInteractionValidator
      .createFor(resolveHref(newConfiguration.getHref))
      .build()

    newConfiguration
  }

  /**
    * Resolves relative [[URI]] representations as files relative to the configuration root directory.
    *
    * @param href a [[String]] representation of a [[URI]]
    * @return a [[String]] representation of an absolute [[URI]]
    */
  private def resolveHref(href: String): String = {
    val hrefUri = URI.create(href)
    if (hrefUri.isAbsolute) {
      // The URI is absolute, so return it as-is.
      // This handles hrefs pointing to remote resources (e.g., HTTP, FTP).
      hrefUri.toString
    } else {
      // The URI is not absolute, so process it as a file.
      val hrefFile = new File(href)
      if (hrefFile.isAbsolute) {
        // The file path is absolute, so return the absolute URI for the file path.
        hrefFile.getAbsolutePath
      } else {
        // The file path is relative, so resolve it relative to the configuration directory
        // and return the path.
        new File(configurationRoot, href).toURI.toString
      }
    }
  }
}

object OpenApiValidatorFilter {

  // A mapping from Message keys defined by the validation library to data defining how we will handle those Messages.
  // Note that the path and method checks necessarily occur before all other checks since they are required
  // to resolve the API Operation in the OpenAPI document.
  // If either one fails, no further checks will be performed.
  // As such, the validation order will be (path -> method -> everything else).
  // For that reason, path and method issues are given the highest and second highest priorities respectively.
  final val ValidationIssues: Map[String, ValidationIssue] = Map(
    "validation.request.path.missing" -> ValidationIssue(HttpServletResponse.SC_NOT_FOUND, -2),
    "validation.request.operation.notAllowed" -> ValidationIssue(HttpServletResponse.SC_METHOD_NOT_ALLOWED, -1),
    "validation.request.body.unexpected" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 0),
    "validation.request.body.missing" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 1),
    "validation.request.security.missing" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 2),
    "validation.request.security.invalid" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 3),
    "validation.request.parameter.header.missing" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 4),
    "validation.request.parameter.query.missing" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 5),
    "validation.request.parameter.missing" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 6),
    "validation.request.parameter.enum.invalid" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 7),
    "validation.request.parameter.collection.invalid" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 8),
    "validation.request.parameter.collection.invalidFormat" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 9),
    "validation.request.parameter.collection.tooManyItems" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 10),
    "validation.request.parameter.collection.tooFewItems" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 11),
    "validation.request.parameter.collection.duplicateItems" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 12),
    "validation.request.contentType.invalid" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 13),
    "validation.request.contentType.notAllowed" -> ValidationIssue(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, 14),
    "validation.request.accept.invalid" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 15),
    "validation.request.accept.notAllowed" -> ValidationIssue(HttpServletResponse.SC_NOT_ACCEPTABLE, 16),
    "validation.schema.invalidJson" -> ValidationIssue(HttpServletResponse.SC_BAD_REQUEST, 17),
    "validation.schema.unknownError" -> ValidationIssue(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 18)
  )

  case class ValidationIssue(statusCode: Int, priority: Int)

  case class ValidationFailure(issue: ValidationIssue, message: ValidationReport.Message)

  /**
    * @param httpServletRequest an [[HttpServletRequest]]
    * @return an [[HttpServletRequest]] with a [[ServletInputStream]] that supports
    *         [[java.io.InputStream#mark]] and [[java.io.InputStream#reset]]
    */
  private def getBufferingHttpServletRequest(httpServletRequest: HttpServletRequest): HttpServletRequest = {
    val servletInputStream = httpServletRequest.getInputStream
    if (servletInputStream.markSupported) {
      httpServletRequest
    } else {
      new HttpServletRequestWrapper(httpServletRequest, new BufferedServletInputStream(servletInputStream))
    }
  }

  /**
    * Maps all Error-level Messages to Issues, sorts the Issues by priority, and returns the highest priority Issue,
    * if one is present.
    *
    * Note that a report may contain Messages for more than one failed check.
    *
    * Note that we have chosen to ignore unknown issues (i.e., remove issues that are not present in our issue map).
    * In doing so, we are effectively only supporting issues that we have mapped out ourselves.
    * As a result, the behavior of this filter should be knowable without requiring intimate knowledge
    * of the underlying validation library.
    * Additionally, the underlying validation library can be upgraded while the behavior of this filter
    * remains relatively stable (since any new issues would not necessarily need to be accounted for
    * at the time of the upgrade).
    * Furthermore, this approach provides a way to add functionality iteratively.
    * For example, if this filter fails to map an attribute of the servlet request to a request
    * for validation, the request is passed rather than rejected.
    * In that case, rejection would be inappropriate since the request may satisfy the required
    * criterion, our mapping simply does not provide the information necessary to make an accurate determination.
    *
    * @param validationReport a [[ValidationReport]]
    * @return the highest priority [[ValidationFailure]] if one exists, otherwise [[None]]
    */
  private def getPriorityValidationFailure(validationReport: ValidationReport): Option[ValidationFailure] = {
    validationReport.getMessages.asScala
      .filter(message => message.getLevel == ValidationReport.Level.ERROR)
      .flatMap(message => ValidationIssues.get(message.getKey).map(issue => ValidationFailure(issue, message)))
      .sortBy(issue => issue.issue.priority)
      .headOption
  }
}
