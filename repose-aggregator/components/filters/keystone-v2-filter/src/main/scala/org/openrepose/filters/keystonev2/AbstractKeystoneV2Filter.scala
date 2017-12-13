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
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.{IdentityStatus, OpenStackServiceHeader}
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.config._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

abstract class AbstractKeystoneV2Filter
  extends Filter
    with HttpDelegationManager
    with LazyLogging {

  import AbstractKeystoneV2Filter._

  var keystoneV2Config: KeystoneV2Config = _

  def doAuth(): Try[Unit.type]
  def handleFailures(authResult: Try[Unit.type]): Option[Reject]
  def isInitialized: Boolean

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      /**
        * STATIC REFERENCE TO CONFIG
        */
      val config = keystoneV2Config

      /**
        * DECLARE COMMON VALUES
        */
      lazy val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      lazy val response = new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse], MUTABLE, PASSTHROUGH)
      lazy val tenantFromUri: String =
        Option(config.getTenantHandling.getValidateTenant).flatMap(validateTenantConfig =>
          Option(validateTenantConfig.getUriExtractionRegex).flatMap(uriExtractionRegexList =>
            uriExtractionRegexList.asScala.toStream.map(_.r).flatMap(uriExtractionRegex =>
              request.getRequestURI match {
                case uriExtractionRegex(tenantId, _*) => Option(tenantId)
                case _ => None
              }
            ).headOption
          )).getOrElse(throw UnparseableTenantException("Could not parse tenant from the URI"))

      def isWhitelisted(requestUri: String): Boolean = {
        logger.trace("Comparing request URI to whitelisted URIs")

        val whiteListUris: List[String] = config.getWhiteList.getUriRegex.asScala.toList

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
          val authResult = doAuth()
          handleFailures(authResult).getOrElse {
            authResult match {
              case Success(_) => Pass
              case Failure(e: UnparseableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
              case Failure(e) => Reject(SC_INTERNAL_SERVER_ERROR, Some(e.getMessage))
            }
          }
        }

      filterResult match {
        case Pass =>
          logger.trace("Processing completed, passing to next filter or service")
          addIdentityStatusHeader(confirmed = true)
          chain.doFilter(request, response)
        case Reject(statusCode, message) =>
          Option(config.getDelegating) match {
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

      def addIdentityStatusHeader(confirmed: Boolean): Unit = {
        if (Option(config.getDelegating).isDefined) {
          if (confirmed) request.addHeader(OpenStackServiceHeader.IDENTITY_STATUS, IdentityStatus.CONFIRMED)
          else request.addHeader(OpenStackServiceHeader.IDENTITY_STATUS, IdentityStatus.INDETERMINATE)
        }
      }
    }
  }

}

object AbstractKeystoneV2Filter {

  sealed trait KeystoneV2Result
  object Pass extends KeystoneV2Result
  case class Reject(status: Int, message: Option[String] = None) extends KeystoneV2Result

}
