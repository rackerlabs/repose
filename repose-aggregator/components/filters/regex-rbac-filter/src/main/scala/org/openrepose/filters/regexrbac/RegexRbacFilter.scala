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
package org.openrepose.filters.regexrbac

import java.io.InputStream
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateFailedException
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.commons.utils.string.{RegexString, RegexStringOperators}
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.regexrbac.RegexRbacFilter._
import org.openrepose.filters.regexrbac.config.RegexRbacConfig

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try

@Named
class RegexRbacFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[RegexRbacConfig](configurationService)
    with HttpDelegationManager
    with RegexStringOperators
    with LazyLogging {

  override val DEFAULT_CONFIG: String = "regex-rbac.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/schema/config/regex-rbac.xsd"

  var parsedResources: Option[List[Resource]] = None

  override def doWork(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain): Unit = {
    val httpRequestWrapper = new HttpServletRequestWrapper(httpRequest)

    def sendError(message: String, statusCode: Int): Unit = {
      val status = if (configuration.isMaskRaxRoles403) {
        logger.debug(s"Masking $statusCode with $SC_NOT_FOUND")
        SC_NOT_FOUND
      } else statusCode

      Option(configuration.getDelegating) match {
        case Some(delegating) =>
          logger.debug(s"Delegating with status $status caused by: $message")
          val delegationHeaders = buildDelegationHeaders(
            status,
            Option(delegating.getComponentName).getOrElse("regex-rbac"),
            s"Failed in the RegEx RBAC filter due to $message",
            delegating.getQuality)
          delegationHeaders foreach { case (key, values) =>
            values foreach { value =>
              httpRequestWrapper.addHeader(key, value)
            }
          }
          chain.doFilter(httpRequestWrapper, httpResponse)
        case None =>
          logger.debug(s"Rejecting with status $status caused by: $message")
          httpResponse.sendError(status)
      }
    }

    val requestUri = httpRequestWrapper.getRequestURI
    val matchedPaths = parsedResources.flatMap(resources => Option(resources
      .filter { resource =>
        resource.path =~ requestUri
      }
    )).getOrElse(List.empty[Resource])
    if (matchedPaths.isEmpty) {
      sendError("No Matching Paths", SC_NOT_FOUND)
    } else {
      val requestMethod = httpRequestWrapper.getMethod.toUpperCase()
      val matchedMethods = matchedPaths.filter(resource =>
        resource.methods.contains("ANY") ||
          resource.methods.contains("ALL") ||
          resource.methods.contains(requestMethod))
      if (matchedMethods.isEmpty) {
        sendError("No Matching Methods", SC_METHOD_NOT_ALLOWED)
      } else {
        val requestRoles = httpRequestWrapper.getSplittableHeaders("X-Roles").asScala.toSet
        val (relevantRoles, noMatchingRoles) = matchedMethods.foldLeft(Set.empty[String], List.empty[Resource]) {
          case ((matches, noMatch), p) => p match {
            case resource if resource.roles.contains("ANY") => (Set("ANY") ++ matches, noMatch)
            case resource if resource.roles.contains("ALL") => (Set("ALL") ++ matches, noMatch)
            case resource if resource.roles.intersect(requestRoles).nonEmpty => (resource.roles.intersect(requestRoles) ++ matches, noMatch)
            case resource => (matches, resource :: noMatch)
          }
        }
        if (noMatchingRoles.nonEmpty) {
          sendError("Non-Matching Roles", SC_FORBIDDEN)
        } else {
          httpRequestWrapper.addHeader(XRelevantRolesHeader, relevantRoles.mkString(", "))
          chain.doFilter(httpRequestWrapper, httpResponse)
        }
      }
    }
  }

  override def doConfigurationUpdated(newConfigurationObject: RegexRbacConfig): Unit = {

    def parseLine(line: String): Option[Resource] = {
      val values = line.trim.split("\\s+")
      values.length match {
        case x if x > 3 =>
          logger.warn(s"Malformed RBAC Resource: $line")
          logger.info("Ensure all roles with spaces have been modified to use a non-breaking space (NBSP, &#xA0;) character.")
          throw new UpdateFailedException("Malformed RBAC Resource")
        case 3 =>
          Some(Resource(
            stringToRegexString(values(0)),
            Try(values(1).split(',').toSet[String].map(_.trim) map (_.toUpperCase)).getOrElse(Set.empty),
            Try(values(2).split(',').toSet[String].map(_.trim).map(_.replaceAll("\u00A0", " "))
              .map(role => if (role.equalsIgnoreCase("ANY") || role.equalsIgnoreCase("ALL")) role.toUpperCase() else role)
            ).getOrElse(Set.empty)
          ))
        case 1 if values(0).length == 0 =>
          None
        case _ =>
          logger.warn(s"Malformed RBAC Resource: $line")
          throw new UpdateFailedException("Malformed RBAC Resource")
      }
    }

    def readResource(resourceStream: InputStream): Option[String] = {
      Try(Some(Source.fromInputStream(resourceStream).getLines().mkString("\n"))).getOrElse(None)
    }

    val rawResources = Option(newConfigurationObject.getResources).flatMap { resources =>
      Option(resources.getValue).filter(_.trim.nonEmpty).orElse(
        Option(resources.getHref).flatMap { fileName =>
          readResource(
            configurationService.getResourceResolver.resolve(fileName).newInputStream()
          )
        }
      )
    }

    parsedResources = rawResources.flatMap { lines =>
      Some(lines.replaceAll("[\r?\n?]", "\n").split('\n').toList.flatMap(parseLine))
    }
  }
}

object RegexRbacFilter {
  private final val XRelevantRolesHeader = "X-Relevant-Roles"

  case class Resource(path: RegexString, methods: Set[String], roles: Set[String])

}
