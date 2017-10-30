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
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateFailedException
import org.openrepose.commons.utils.string.RegexStringOperators
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.regexrbac.RegexRbacFilter._
import org.openrepose.filters.regexrbac.config.RegexRbacConfig

import scala.io.Source
import scala.util.Try
import scala.util.matching.Regex

@Named
class RegexRbacFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[RegexRbacConfig](configurationService) with RegexStringOperators with LazyLogging {

  override val DEFAULT_CONFIG: String = "regex-rbac.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/schema/config/regex-rbac.xsd"

  var parsedResources: Option[List[Resource]] = None

  override def doWork(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain): Unit = {
    chain.doFilter(httpRequest, httpResponse)
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
            values(0).r,
            Try(values(1).split(',').toSet[String].map(_.trim)).getOrElse(Set.empty),
            Try(values(2).split(',').toSet[String].map(_.trim).map(_.replaceAll("&#xA0;", " "))).getOrElse(Set.empty)
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

  case class Resource(path: Regex, methods: Set[String], roles: Set[String])

}
