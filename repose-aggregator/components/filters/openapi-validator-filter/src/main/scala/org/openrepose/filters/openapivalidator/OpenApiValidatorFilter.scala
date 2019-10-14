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
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.filters.openapivalidator.HttpServletValidatorRequest.RequestConversionException
import org.openrepose.filters.openapivalidator.OpenApiValidatorFilter._
import org.openrepose.filters.openapivalidator.config.OpenApiValidatorConfig
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

/**
  * This filter will validate requests against an OpenAPI document.
  */
@Named
class OpenApiValidatorFilter @Inject()(@Value(ReposeSpringProperties.CORE.CONFIG_ROOT) configurationRoot: String,
                                       configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[OpenApiValidatorConfig](configurationService) with StrictLogging {

  override final val DEFAULT_CONFIG: String = "openapi-validator.cfg.xml"
  override final val SCHEMA_LOCATION: String = "/META-INF/schema/config/openapi-validator.xsd"

  private[openapivalidator] var validator: OpenApiInteractionValidator = _

  override def doWork(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain): Unit = {
    // Ensure that request input stream buffering is supported.
    val bufferingHttpServletRequest = getBufferingHttpServletRequest(httpRequest)

    bufferingHttpServletRequest.getInputStream.mark(Integer.MAX_VALUE)

    logger.trace("Wrapping the servlet request in a validation library request")
    val validationRequest = HttpServletValidatorRequest(bufferingHttpServletRequest)

    logger.trace("Validating request")
    val tryValidationReport = Try(validator.validateRequest(validationRequest))

    bufferingHttpServletRequest.getInputStream.reset()

    tryValidationReport match {
      case Success(validationReport) =>
        getPriorityValidationFailure(validationReport) match {
          case Some(validationFailure) =>
            logger.debug("Failed to validate request -- rejecting with status code: '{}' for reason: '{}'", validationFailure.statusCode.toString, validationFailure.message)
            httpResponse.sendError(validationFailure.statusCode, validationFailure.message.getMessage)
          case None =>
            logger.trace("Successfully validated request -- continuing processing")
            chain.doFilter(bufferingHttpServletRequest, httpResponse)
        }
      case Failure(e: RequestConversionException) =>
        logger.error("Failed to convert the servlet request to a validation request", e)
        httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
      case Failure(e) =>
        val statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        logger.error("An unexpected issue arose while validating the request -- rejecting with status code: '{}' for reason '{}'", statusCode.toString, e.getMessage, e)
        httpResponse.sendError(statusCode, e.getMessage)
    }
  }

  override def doConfigurationUpdated(newConfiguration: OpenApiValidatorConfig): OpenApiValidatorConfig = {
    // Setting the current thread's context ClassLoader to the ClassLoader that loaded this class.
    // This is a hack to get the OpenAPIParser's ServiceLoader to load the Swagger v2 parser.
    // Without this hack, we cannot support Swagger v2 documents.
    //
    // Part of the issue is that this code is executed on the Thread created by the ConfigurationService,
    // and the executing Thread's context ClassLoader is what the ServiceLoader uses to load a resource
    // which defines services.
    // Well, the ConfigurationService Thread does not have a context ClassLoader set, so the system ClassLoader
    // is used instead, but due to the way filters are loaded, the system ClassLoader cannot access the same
    // resources as the ClassLoader used to load this filter.
    val contextClassLoader = Thread.currentThread.getContextClassLoader
    Thread.currentThread.setContextClassLoader(classOf[OpenApiValidatorFilter].getClassLoader)

    validator = OpenApiInteractionValidator
      .createFor(resolveHref(newConfiguration.getHref))
      .build()

    Thread.currentThread.setContextClassLoader(contextClassLoader)

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
  // This mapping is prioritized with higher priority issues being listed before lower priority issues.
  // In the case of multiple validation issues for a single request, the highest priority issue will be used to
  // craft a response.
  // Note that since this Map is ordered, lookups are not performed in constant time.
  // Note that the path and method checks necessarily occur before all other checks since they are required
  // to resolve the API Operation in the OpenAPI document.
  // If either one fails, no further checks will be performed.
  // As such, the validation order will be (path -> method -> everything else).
  // For that reason, path and method issues are given the highest priorities.
  final val ValidationIssues: Map[String, Int] = ListMap(
    "validation.request.path.missing" -> HttpServletResponse.SC_NOT_FOUND,
    "validation.request.operation.notAllowed" -> HttpServletResponse.SC_METHOD_NOT_ALLOWED,
    "validation.request.body.unexpected" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.body.missing" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.security.missing" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.security.invalid" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.header.missing" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.query.missing" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.missing" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.enum.invalid" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.collection.invalid" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.collection.invalidFormat" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.collection.tooManyItems" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.collection.tooFewItems" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.parameter.collection.duplicateItems" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.contentType.invalid" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.contentType.notAllowed" -> HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
    "validation.request.accept.invalid" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.request.accept.notAllowed" -> HttpServletResponse.SC_NOT_ACCEPTABLE,
    "validation.schema.invalidJson" -> HttpServletResponse.SC_BAD_REQUEST,
    "validation.schema.unknownError" -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR
  )

  case class ValidationFailure(statusCode: Int, message: ValidationReport.Message)

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
    * Finds the highest priority Error-level Message from a [[ValidationReport]].
    *
    * Note that a report may contain Messages for more than one failed check.
    *
    * Note that unknown issues (i.e., issues that are not present in our issue map) will cause
    * a general-purpose [[ValidationFailure]] to be returned.
    * In this way, we are failing for any validation error in the report.
    * The validation library is considered the source of truth for what validations are supported.
    *
    * @param validationReport a [[ValidationReport]]
    * @return the highest priority [[ValidationFailure]] if one exists, otherwise [[None]]
    */
  private def getPriorityValidationFailure(validationReport: ValidationReport): Option[ValidationFailure] = {
    val errorMessages = validationReport.getMessages.asScala
      .filter(message => message.getLevel == ValidationReport.Level.ERROR)
      .map(message => message.getKey -> message)
      .toMap

    if (errorMessages.isEmpty) {
      None
    } else {
      ValidationIssues
        .find({ case (issueKey, _) => errorMessages.contains(issueKey) })
        .orElse(Some(errorMessages.keys.head, HttpServletResponse.SC_BAD_REQUEST))
        .map({ case (issueKey, statusCode) => ValidationFailure(statusCode, errorMessages(issueKey)) })
    }
  }
}
