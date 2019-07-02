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
package org.openrepose.filters.keystonev2

import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.utils.http.{IdentityStatus, OpenStackServiceHeader}
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.keystonev2.config._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

abstract class AbstractKeystoneV2Filter[T <: KeystoneV2Config: ClassTag](configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[T](configurationService)
    with HttpDelegationManager
    with StrictLogging {

  import AbstractKeystoneV2Filter._

  val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result]

  def doAuth(request: HttpServletRequestWrapper): Try[Unit.type]

  def doWork(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, chain: FilterChain): Unit = {
    /**
      * DECLARE COMMON VALUES
      */
    lazy val request = new HttpServletRequestWrapper(servletRequest)
    lazy val response = new HttpServletResponseWrapper(servletResponse, MUTABLE, PASSTHROUGH)

    def isWhitelisted(requestUri: String): Boolean = {
      logger.trace("Comparing request URI to whitelisted URIs")

      val whiteListUris: List[String] = configuration.getWhiteList.getUriRegex.asScala.toList

      whiteListUris exists { pattern =>
        logger.debug(s"checking $requestUri against $pattern")
        requestUri.matches(pattern)
      }
    }

    /**
      * BEGIN PROCESSING
      */
    logger.debug("Keystone v2 filter processing request...")

    val filterResult =
      if (isWhitelisted(request.getRequestURI)) {
        Pass
      } else {
        val authResult = doAuth(request)
        handleFailures.applyOrElse(authResult, (_: Try[Unit.type]) match {
          case Success(_) => Pass
          case Failure(e) => Reject(SC_INTERNAL_SERVER_ERROR, Some(e.getMessage))
        })
      }

    filterResult match {
      case Pass =>
        logger.trace("Processing completed, passing to next filter or service")
        addIdentityStatusHeader(confirmed = true)
        chain.doFilter(request, response)
      case Reject(statusCode, message, headers) =>
        headers foreach { case (name, value) =>  response.addHeader(name, value) }
        Option(configuration.getDelegating) match {
          case Some(delegating) =>
            logger.debug(s"Delegating with status $statusCode caused by: ${message.getOrElse("unspecified")}")

            val delegationHeaders = buildDelegationHeaders(statusCode,
              "keystone-v2",
              message.getOrElse("Failure in the Keystone v2 filter").replace("\n", " "),
              delegating.getQuality)

            addIdentityStatusHeader(confirmed = false)
            delegationHeaders foreach { case (key, values) =>
              values foreach { value =>
                request.addHeader(key, value)
              }
            }

            chain.doFilter(request, response)

            logger.trace(s"Processing response with status code: $statusCode")

          case None =>
            logger.debug(s"Rejecting with status $statusCode")

            message match {
              case Some(m) =>
                logger.debug(s"Rejection message: $m")
                response.sendError(statusCode, m)
              case None => response.sendError(statusCode)
            }
        }
    }

    response.commitToResponse()

    def addIdentityStatusHeader(confirmed: Boolean): Unit = {
      if (Option(configuration.getDelegating).isDefined) {
        if (confirmed) request.addHeader(OpenStackServiceHeader.IDENTITY_STATUS, IdentityStatus.CONFIRMED)
        else request.addHeader(OpenStackServiceHeader.IDENTITY_STATUS, IdentityStatus.INDETERMINATE)
      }
    }
  }

  override def doConfigurationUpdated(configurationObject: T): T = {
    // Fix JAXB defaults
    if (Option(configurationObject.getTenantHandling).isEmpty) {
      configurationObject.withTenantHandling(new TenantHandlingType())
    }
    if (Option(configurationObject.getWhiteList).isEmpty) {
      configurationObject.withWhiteList(new WhiteListType())
    }

    configurationObject
  }
}

object AbstractKeystoneV2Filter {

  sealed trait KeystoneV2Result
  object Pass extends KeystoneV2Result
  case class Reject(status: Int, message: Option[String] = None, headers: Map[String, String] = Map.empty) extends KeystoneV2Result

}
