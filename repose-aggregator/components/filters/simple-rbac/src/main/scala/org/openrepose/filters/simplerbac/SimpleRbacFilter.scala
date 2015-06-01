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
package org.openrepose.filters.simplerbac

import java.io.InputStream
import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.http.HttpServletResponse._

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.simplerbac.config.SimpleRbacConfig

import scala.collection.JavaConversions._
import scala.io.Source
import scala.util.Try

@Named
class SimpleRbacFilter @Inject()(configurationService: ConfigurationService)
  extends Filter
  with UpdateListener[SimpleRbacConfig]
  with HttpDelegationManager
  with LazyLogging {

  private final val DEFAULT_CONFIG = "simple-rbac.cfg.xml"

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: SimpleRbacConfig = _
  var initialized = false
  var resources: List[Resource] = _

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configurationFile)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/simple-rbac.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[SimpleRbacConfig]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("Simple RBAC filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(500)
    } else {
      val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
      val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

      logger.trace("Simple RBAC filter processing request...")
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // IF any Resources satisfy the Request,                                //
      //    THEN continue with a status code of OK (200);                     //
      // ELSE:                                                                //
      //    IF no Resources satisfy the Request's Path,                       //
      //       THEN return with a status code of NOT FOUND (404);             //
      //    ELSE IF any Resources satisfy the Request's Roles, THEN:          //
      //       IF Mask Roles is Enabled,                                      //
      //          THEN return with a status code of METHOD NOT ALLOWED (405); //
      //       ELSE return with a status code of FORBIDDEN (403).             //
      //    ELSE no Resources satisfy the Request's Method, SO:               //
      //       IF Mask Roles is Enabled,                                      //
      //          THEN return with a status code of NOT FOUND (404);          //
      //       ELSE return with a status code of FORBIDDEN (403).             //
      //////////////////////////////////////////////////////////////////////////
      val resourceRequest = new ResourceRequest(
        mutableHttpRequest.getRequestURI,
        mutableHttpRequest.getMethod,
        mutableHttpRequest.getHeaders(configuration.getRolesHeaderName).toSet[String].flatMap(parseMethodsRoles)
      )
      if (resources.exists(_.satisfiesRequest(resourceRequest))) {
        mutableHttpResponse.setStatus(SC_OK) // 200
      } else {
        val paths = resources.filter(_.satisfiesPath(resourceRequest))
        if (paths.isEmpty) {
          mutableHttpResponse.setStatus(SC_NOT_FOUND) // 404
        } else {
          if (paths.exists(_.satisfiesRoles(resourceRequest))) {
            if (configuration.isEnableMasking403S) {
              mutableHttpResponse.setStatus(SC_METHOD_NOT_ALLOWED) // 405
            } else {
              mutableHttpResponse.setStatus(SC_FORBIDDEN) // 403
            }
          } else {
            if (configuration.isEnableMasking403S) {
              mutableHttpResponse.setStatus(SC_NOT_FOUND) // 404
            } else {
              mutableHttpResponse.setStatus(SC_FORBIDDEN) // 403
            }
          }
        }
      }
      //////////////////////////////////////////////////////
      //mutableHttpResponse.setStatus(SC_NOT_IMPLEMENTED) // 501
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      if (mutableHttpResponse.getStatus == SC_OK) {
        logger.trace("Simple RBAC filter passing request...")
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
        logger.trace("Simple RBAC filter handling response...")
      }
    }
    logger.trace("Simple RBAC filter returning response...")
  }

  override def configurationUpdated(configurationObject: SimpleRbacConfig): Unit = {
    configuration = configurationObject
    resources = parseResources(configuration.getResources).getOrElse(
      if (configuration.getResourcesFileName != null) {
        parseResources(
          readResource(
            configurationService.getResourceResolver.resolve(configuration.getResourcesFileName).newInputStream()
          ).getOrElse("")
        ).getOrElse(List.empty)
      } else {
        List.empty
      }
    )
    initialized = true
  }

  override def isInitialized: Boolean = initialized

  case class ResourceRequest(path: String, method: String, roles: Set[String])

  case class Resource(path: String, methods: Set[String], roles: Set[String]) {
    def satisfiesRequest(request: ResourceRequest): Boolean = {
      satisfiesPath(request) &&
        satisfiesMethods(request) &&
        satisfiesRoles(request)
    }

    def satisfiesPath(request: ResourceRequest): Boolean = {
      this.path == request.path
    }

    def satisfiesMethods(request: ResourceRequest): Boolean = {
      this.methods.contains("ANY") ||
        this.methods.contains("ALL") ||
        this.methods.contains(request.method)
    }

    def satisfiesRoles(request: ResourceRequest): Boolean = {
      this.roles.contains("ANY") ||
        this.roles.contains("ALL") ||
        this.roles.intersect(request.roles).nonEmpty
    }
  }

  def readResource(resourceStream: InputStream): Option[String] = {
    Try(Some(Source.fromInputStream(resourceStream).getLines().mkString("\n"))).getOrElse(None)
  }

  private def parseResources(lines: String): Option[List[Resource]] = {
    if (lines == null) {
      None
    } else {
      Some(lines.replaceAll("[\r?\n?]", "\n").split('\n').toList.map(parseLine(_).orNull).filter(_ != null))
    }
  }

  private def parseLine(line: String): Option[Resource] = {
    val values = line.split("\\s+")
    if (values.length == 3) {
      Some(new Resource(values(0), parseMethodsRoles(values(1)), parseMethodsRoles(values(2))))
    } else {
      logger.warn(s"Malformed RBAC Resource: $line")
      None
    }
  }

  private def parseMethodsRoles(value: String): Set[String] = {
    Try(value.split(',').toSet[String].map(_.trim)).getOrElse(Set.empty)
  }
}
